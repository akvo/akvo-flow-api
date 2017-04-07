(ns org.akvo.flow-api.endpoint.anomaly)

(defmulti handle
  (fn [e]
    (:status (ex-data e))))

(defmethod handle :default [e]
  (throw e))

(defmethod handle :unauthorized [e]
  {:status 401
   :body (assoc (dissoc (ex-data e) :status)
                :message (.getMessage e))})

(defmethod handle :not-found [e]
  {:status 404
   :body (assoc (dissoc (ex-data e) :status)
                :message (.getMessage e))})
