(ns org.akvo.flow-api.middleware.anomaly
  (:require [org.akvo.flow-api.endpoint.anomaly :as anomaly]
            [org.akvo.flow-api.anomaly :as an]
            [clojure.tools.logging :as log]
            [clojure.stacktrace :as stacktrace])
  (:import [clojure.lang ExceptionInfo]))

(defn wrap-anomaly [handler]
  (fn [request]
    (try
      (handler request)
      (catch ExceptionInfo e
        (anomaly/handle e)))))

(defn translate-exception [e]
  (condp #(.contains %2 %1) (or (.getMessage e) "")
    "Over Quota" (an/too-many-requests)
    "required more quota" (an/too-many-requests)
    "Please try again in 30 seconds" (an/bad-gateway)
    (throw e)))

(defn wrap-log-errors [handler]
  (fn [request]
    (try
      (handler request)
      (catch Throwable e
        (stacktrace/print-stack-trace e)
        (log/error e (str "Error:" (.getMessage e)))
        (translate-exception e)))))
