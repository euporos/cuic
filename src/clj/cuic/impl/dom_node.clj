(ns cuic.impl.dom-node
  (:require [clojure.string :as string]
            [cuic.impl.exception :as ex :refer [call]]
            [cuic.impl.js-bridge :as js]
            [cuic.impl.browser :refer [tools]])
  (:import (java.io Writer)))

(defrecord DOMNode [id browser]
  Object
  (toString [this]
    (try
      (let [attrs (->> (call (-> (.getDOM (tools browser))
                                 (.getAttributes id)))
                       (partition 2 2)
                       (map (fn [[k v]] [(keyword k) v]))
                       (into {}))
            cls   (as-> (get attrs :class "") $
                        (string/split $ #"\s+")
                        (map string/trim $)
                        (remove string/blank? $)
                        (set $)
                        (sort $))
            id    (some->> (:id attrs) (string/trim) (str "#"))
            tag   (some-> (js/eval-in this "this.tagName") (string/lower-case))
            s     (str tag id (string/join "." (cons "" cls)))]
        (if (empty? s)
          (str "<!node id=" id ">")
          (str "<" s ">")))
      (catch Exception e
        (str "<!!error \"" (.getMessage e) "\">")))))

(defmethod print-method DOMNode [node ^Writer w]
  (.write w (.toString node)))

(defn maybe [x]
  (cond
    (instance? DOMNode x) x
    (sequential? x)
    (if (<= (count x) 1)
      (maybe (first x))
      (throw (ex/retryable "Node list can't contain more than one node" {:list (vec x)})))
    (nil? x) nil
    :else (throw (ex/retryable "Value is not a valid DOM node" {:value x}))))

(defn existing [x]
  (cond
    (instance? DOMNode x) x
    (sequential? x)
    (case (count x)
      1 (existing (first x))
      0 (throw (ex/retryable "Node list can't be empty"))
      (throw (ex/retryable "Node list must contain exactly one node" {:list (vec x)})))
    :else (throw (ex/retryable "Value is not a valid DOM node" {:value x}))))

(defn visible [x]
  (cond
    (instance? DOMNode x) (if (js/eval-in x "!!this.offsetParent") x)
    (sequential? x)
    (case (count x)
      1 (existing (first x))
      0 (throw (ex/retryable "Node list can't be empty"))
      (throw (ex/retryable "Node list must contain exactly one node" {:list (vec x)})))
    :else (throw (ex/retryable "Value is not a valid DOM node" {:value x}))))


