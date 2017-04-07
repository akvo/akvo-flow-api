(ns org.akvo.flow-api.middleware.email)

(defn wrap-email [handler]
  (fn [request]
    (if-let [email (get-in request [:headers "x-akvo-email"])]
      (handler (assoc request :email email))
      (handler request))))
