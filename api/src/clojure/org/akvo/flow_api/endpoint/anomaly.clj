(ns org.akvo.flow-api.endpoint.anomaly
  (:require [ring.util.response :refer [response]]))

(defmulti handle
  (fn [e]
    (:status (ex-data e))))

(defmethod handle :default [e]
  (throw e))

(defn body [e]
  (-> (ex-data e)
      (assoc :message (.getMessage e))
      (dissoc :status)))

(defmethod handle :unauthorized [e]
  (-> (body e)
      (response)
      (assoc :status 403)))

(defmethod handle :not-found [e]
  (-> (body e)
      (response)
      (assoc :status 404)))
