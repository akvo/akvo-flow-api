(ns org.akvo.flow-api.endpoint.anomaly
  (:require [ring.util.response :refer [response]]))

(defmulti handle
  (fn [e]
    (:org.akvo.flow-api/anomaly (ex-data e))))

(defmethod handle :default [e]
  (throw e))

(defn body [e]
  (-> (ex-data e)
      (assoc :message (.getMessage e))
      (dissoc :org.akvo.flow-api/anomaly)))

(defmethod handle :org.akvo.flow-api.anomaly/unauthorized [e]
  (-> (body e)
      (response)
      (assoc :status 403)))

(defmethod handle :org.akvo.flow-api.anomaly/not-found [e]
  (-> (body e)
      (response)
      (assoc :status 404)))

(defmethod handle :org.akvo.flow-api.anomaly/bad-request [e]
  (-> (body e)
      (response)
      (assoc :status 400)))

(defmethod handle :org.akvo.flow-api.anomaly/too-many-requests [e]
  (-> (body e)
      (response)
      (assoc :status 429)))

(defmethod handle :org.akvo.flow-api.anomaly/bad-gateway [e]
  (-> (body e)
      (response)
      (assoc :status 502)))
