(ns org.akvo.flow-api.anomaly)

(defn throw-anomaly [key message m]
  {:pre [(keyword? key)
         (string? message)
         (map? m)]}
  (throw (ex-info message (assoc m :org.akvo.flow-api/anomaly key))))

(defn not-found [message m]
  (throw-anomaly ::not-found message m))

(defn unauthorized [message m]
  (throw-anomaly ::unauthorized message m))

(defn bad-request [message m]
  (throw-anomaly ::bad-request message m))

(defn too-many-requests [message m]
  (throw-anomaly ::too-many-requests message m))

(defn bad-gateway [message m]
  (throw-anomaly ::bad-gateway message m))
