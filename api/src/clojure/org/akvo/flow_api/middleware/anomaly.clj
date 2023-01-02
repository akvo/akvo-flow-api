(ns org.akvo.flow-api.middleware.anomaly
  (:require [org.akvo.flow-api.endpoint.anomaly :as anomaly]
            [org.akvo.flow-api.anomaly :as an]
            #_[clojure.tools.logging :as log]
            #_[clojure.stacktrace :as stacktrace])
  (:import [clojure.lang ExceptionInfo]))

(defn wrap-anomaly [handler]
  (fn [request]
    (try
      (handler request)
      (catch ExceptionInfo e
        (anomaly/handle e)))))

(defn translate-exception [^Throwable e]
  (condp #(.contains ^String %2 ^String %1) (or (.getMessage e) "")
    "Over Quota" (an/too-many-requests)
    "required more quota" (an/too-many-requests)
    "Please try again in 30 seconds" (an/bad-gateway)
    ((println (str "The message" e))
    (throw e))))

(defn wrap-log-errors [handler]
  (fn [request]
    (try
      (handler request)
      (catch Throwable e
        #_(stacktrace/print-stack-trace e)
        #_(log/error e (str "Error:" (.getMessage e)))
        (translate-exception e)))))
