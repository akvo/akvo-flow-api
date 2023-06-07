(ns org.akvo.flow-api.endpoint.device
  (:require [compojure.core :refer [GET]]
            [org.akvo.flow-api.boundary.device :as device]
            [org.akvo.flow-api.boundary.user :as user]
            [org.akvo.flow-api.middleware.resolve-alias :refer [wrap-resolve-alias]]
            [org.akvo.flow-api.middleware.jdo-persistent-manager :as jdo-pm]
            [ring.util.response :refer [response]]
            [org.akvo.flow-api.datastore :as ds]))

(defn devices-response [devices]
  (response {:devices devices}))

(defn endpoint* [{:keys [remote-api]}]
  (GET "/devices" {:keys [email instance-id]}
    (ds/with-remote-api remote-api instance-id
      (user/id-by-email-or-throw-error remote-api instance-id email)
      (->
        (device/list)
        (devices-response)))))

(defn endpoint [{:keys [akvo-flow-server-config] :as deps}]
  (-> (endpoint* deps)
      (wrap-resolve-alias akvo-flow-server-config)
      (jdo-pm/wrap-close-persistent-manager)))
