(ns ^:no-doc cuic.internal.page
  (:require [clojure.tools.logging :refer [warn]]
            [cuic.internal.cdt :refer [invoke on off cdt-promise]])
  (:import (java.lang AutoCloseable)))

(set! *warn-on-reflection* true)

(defrecord Page [cdt state subscription]
  AutoCloseable
  (close [_]
    (reset! state nil)
    (off subscription)))

(defn- main-frame-id [page]
  (get-in @(:state page) [:main-frame :id]))

(defn- loader-id [page]
  (get-in @(:state page) [:main-frame :loaderId]))

(defn- handle-lifecycle-event [state {:keys [name frameId loaderId]}]
  (letfn [(handle [{:keys [main-frame] :as s}]
            (if (= (:id main-frame) frameId)
              (if (= "init" name)
                (-> (assoc s :events #{})
                    (assoc-in [:main-frame :loaderId] loaderId))
                (update s :events conj name))
              s))]
    (swap! state handle)))

(defn- handle-dialog-opening [cdt state {:keys [message type defaultPrompt]}]
  (if-let [handler (:dialog-handler @state)]
    (let [args {:message        message
                :type           (keyword type)
                :default-prompt defaultPrompt}
          result (handler args)]
      (invoke {:cdt  cdt
               :cmd  "Page.handleJavaScriptDialog"
               :args {:accept     (boolean result)
                      :promptText (if (string? result) result "")}}))
    (do (warn "JavaScript dialog was opened but no handler is defined."
              "Using default handler accepting all dialogs")
        (invoke {:cdt  cdt
                 :cmd  "Page.handleJavaScriptDialog"
                 :args {:accept     true
                        :promptText ""}}))))

(defn- handle-event [cdt state method params]
  (case method
    "Page.lifecycleEvent" (handle-lifecycle-event state params)
    "Page.javascriptDialogOpening" (handle-dialog-opening cdt state params)
    nil))

(defn- navigate [{:keys [cdt] :as page} navigation-op timeout]
  (let [p (cdt-promise cdt timeout)
        frame-id (main-frame-id page)
        cb (fn [method {:keys [frameId name]}]
             (when (or (and (= "Page.lifecycleEvent" method)
                            (= frameId frame-id)
                            (= "load" name))
                       (and (= "Page.navigatedWithinDocument" method)
                            (= frameId frame-id)))
               (deliver p ::ok)))
        subs (on {:cdt      cdt
                  :methods  #{"Page.lifecycleEvent"
                              "Page.navigatedWithinDocument"}
                  :callback cb})]
    (try
      (navigation-op)
      (when (pos-int? timeout)
        @p)
      (finally
        (off subs)))))

;;;;

(defn page? [x]
  (some? x))

(defn attach [cdt]
  (let [;; Get initial main frame
        main-frame (-> (invoke {:cdt  cdt
                                :cmd  "Page.getFrameTree"
                                :args {}})
                       (get-in [:frameTree :frame]))
        ;; Initialize page state
        state (atom {:main-frame main-frame :events #{}})
        ;; Attach listeners for lifecycle events
        subs (on {:cdt      cdt
                  :methods  #{"Page.lifecycleEvent"
                              "Page.javascriptDialogOpening"
                              "Page.javascriptDialogClosed"}
                  :callback #(handle-event cdt state %1 %2)})]
    ;; Enable events
    (invoke {:cdt  cdt
             :cmd  "Page.enable"
             :args {}})
    (invoke {:cdt  cdt
             :cmd  "Page.setLifecycleEventsEnabled"
             :args {:enabled true}})
    ;; Return handle to the page
    (->Page cdt state subs)))

(defn set-dialog-handler [page f]
  {:pre [(page? page)
         (fn? f)]}
  (swap! (:state page) #(when % (assoc % :dialog-handler f))))

(defn detach [page]
  {:pre [(page? page)]}
  (.close ^Page page))

(defn detached? [page]
  {:pre [(page? page)]}
  (nil? @(:state page)))

(defn navigate-to [{:keys [cdt] :as page} url timeout]
  {:pre [(page? page)
         (string? url)
         (nat-int? timeout)]}
  (let [op #(invoke {:cdt  cdt
                     :cmd  "Page.navigate"
                     :args {:url url}})]
    (navigate page op timeout)))

(defn navigate-back [{:keys [cdt] :as page} timeout]
  {:pre [(page? page)
         (nat-int? timeout)]}
  (let [history (invoke {:cdt  cdt
                         :cmd  "Page.getNavigationHistory"
                         :args {}})
        idx (:currentIndex history)
        entry (->> (take idx (:entries history))
                   (last))
        op #(invoke {:cdt  cdt
                     :cmd  "Page.navigateToHistoryEntry"
                     :args {:entryId (:id entry)}})]
    (when entry
      (navigate page op timeout))))

(defn navigate-forward [{:keys [cdt] :as page} timeout]
  {:pre [(page? page)
         (nat-int? timeout)]}
  (let [history (invoke {:cdt  cdt
                         :cmd  "Page.getNavigationHistory"
                         :args {}})
        idx (:currentIndex history)
        entry (->> (drop (inc idx) (:entries history))
                   (first))
        op #(invoke {:cdt  cdt
                     :cmd  "Page.navigateToHistoryEntry"
                     :args {:entryId (:id entry)}})]
    (when entry
      (navigate page op timeout))))

(defn get-page-cdt [page]
  {:pre [(page? page)]}
  (:cdt page))

(defn get-page-loader-id [page]
  {:pre [(page? page)]}
  (loader-id page))
