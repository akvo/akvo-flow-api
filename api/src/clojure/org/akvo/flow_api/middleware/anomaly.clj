(ns org.akvo.flow-api.middleware.anomaly
  (:require [org.akvo.flow-api.endpoint.anomaly :as anomaly]
            [org.akvo.flow-api.anomaly :as an])
  (:import [clojure.lang ExceptionInfo]))

(defn wrap-anomaly [handler]
  (fn [request]
    (try
      (handler request)
      (catch ExceptionInfo e
        (anomaly/handle e)))))

(defn translate-exception [e]
  (if (or (.contains (.getMessage e) "Over Quota")
        (.contains (.getMessage e) "required more quota"))
    (an/too-many-requests "This application is temporarily over its serving quota." {})
    (if (.contains (.getMessage e) "Please try again in 30 seconds")
      (an/bad-gateway "The server encountered an error and could not complete your request. Please try again in 30 seconds." {})
      (throw e))))

(defn wrap-log-errors [handler]
  (fn [request]
    (try
      (handler request)
      (catch Throwable e
        (translate-exception e)))))
