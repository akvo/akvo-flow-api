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

(defn wrap-log-errors [handler]
  (fn [request]
    (try
      (handler request)
      (catch Throwable e
        (if (.contains (.getMessage e) "Over Quota")
          (an/too-many-requests "This application is temporarily over its serving quota." {})
          (throw e))))))
