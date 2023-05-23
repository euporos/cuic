(ns ^:no-doc cuic.internal.html
  (:require [clojure.string :as string])
  (:import (org.jsoup Jsoup)
           (org.jsoup.nodes Document Attributes Attribute Comment Element TextNode XmlDeclaration DataNode)))

(set! *warn-on-reflection* true)

(def boolean-attributes
  #{"allowfullscreen"
    "async"
    "autofocus"
    "checked"
    "compact"
    "declare"
    "default"
    "defer"
    "disabled"
    "formnovalidate"
    "hidden"
    "inert"
    "ismap"
    "itemscope"
    "multiple"
    "muted"
    "nohref"
    "noresize"
    "noshade"
    "novalidate"
    "nowrap"
    "open"
    "readonly"
    "required"
    "reversed"
    "seamless"
    "selected"
    "sortable"
    "truespeed"
    "typemustmatch"})

(defmulti to-hiccup type)

(defmethod to-hiccup Attribute [^Attribute a]
  (if (contains? boolean-attributes (.getKey a))
    true
    (.getValue a)))

(defmethod to-hiccup Attributes [^Attributes attrs]
  (->> (seq (.asList attrs))
       (map (fn [^Attribute a] [(keyword (.getKey a)) (to-hiccup a)]))
       (into {})))

(defmethod to-hiccup Element [^Element elem]
  (-> (concat
        [(keyword (.nodeName elem))
         (to-hiccup (.attributes elem))]
        (->> (.childNodes elem)
             (map to-hiccup)
             (remove #(and (string? %) (string/blank? %)))
             (vec)))
      (vec)))

(defmethod to-hiccup TextNode [^TextNode node]
  (.text node))

(defmethod to-hiccup Comment [^Comment comment]
  [:-#comment (.getData comment)])

(defmethod to-hiccup DataNode [^DataNode node]
  [:-#data (.getWholeData node)])

(defmethod to-hiccup XmlDeclaration [^XmlDeclaration decl]
  [:-#declaration (.getWholeDeclaration decl)])

(defmethod to-hiccup Document [^Document doc]
  (to-hiccup (.child doc 0)))

(defmethod to-hiccup :default [x]
  (throw (IllegalStateException. (str "Missing parser: " (type x)))))

(declare parse-document)

(defn- parse-body [^String html]
  (.body (Jsoup/parse html)))

(defn- parse-head [^String html]
  (.head (Jsoup/parse html)))

(defn- parse-fragment [^String html]
  (-> (Jsoup/parseBodyFragment html)
      (.body)
      (.child 0)))

(defn parse-element [^String html]
  (-> (cond
        (re-find #"^<body(\s|>)" html) (parse-body html)
        (re-find #"^<head(\s|>)" html) (parse-head html)
        :else (parse-fragment html))
      (to-hiccup)))

(defn parse-document [^String html]
  (-> (Jsoup/parse html)
      (to-hiccup)))
