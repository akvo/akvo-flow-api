(ns org.akvo.flow-api.endpoint.config-refresh
  (:require
    [compojure.core :refer [POST]]
    [org.akvo.flow-api.component.akvo-flow-server-config :as server-config-component]
    [ring.util.response :refer [response]]))

(defn endpoint* [akvo-flow-server-config]
  (POST "/config-refresh" _
    (future (server-config-component/refresh! akvo-flow-server-config))
    (response "ok")))

(defn endpoint [{:keys [akvo-flow-server-config]}]
  (endpoint* akvo-flow-server-config))
