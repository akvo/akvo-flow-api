(ns org.akvo.flow-api.endpoint.utils
  (:require [clojure.string :as s])
  (:import [java.net.URLEncoder]))

(defn url-encode [s]
  (java.net.URLEncoder/encode (str s) "UTF-8"))

(defn query-params-str [m]
  (->> (for [[k v] m
             :when (some? v)]
         (format "%s=%s" (url-encode k)
                 (url-encode v)))
       (s/join "&")))
