(ns org.akvo.flow-api.middleware.anomaly
  (:require [org.akvo.flow-api.endpoint.anomaly :as anomaly])
  (:import [clojure.lang ExceptionInfo]))

(defn wrap-anomaly [handler]
  (fn [request]
    (try
      (handler request)
      (catch ExceptionInfo e
        (anomaly/handle e)))))
