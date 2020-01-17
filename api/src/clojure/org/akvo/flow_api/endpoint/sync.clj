(ns org.akvo.flow-api.endpoint.sync
  (:require [compojure.core :refer [GET]]
            [org.akvo.flow-api.anomaly :as anomaly]
            [org.akvo.flow-api.endpoint.utils :as utils]
            [org.akvo.flow-api.middleware.jdo-persistent-manager :as jdo-pm]
            [org.akvo.flow-api.middleware.resolve-alias :refer [wrap-resolve-alias]]
            [ring.util.response :refer [response]]))


(defn next-sync-url [api-root instance-id cursor]
  (utils/url-builder api-root instance-id "sync" {"next" true
                                                  "cursor" cursor}))
(defn validate-params
  [params])

(defn endpoint* [deps]
  (GET "/sync" {:keys [alias query-params] :as req}
    (if (empty? query-params)
      (anomaly/bad-request "Missing required parameters" {})
      (if (= "true" (get query-params "initial"))
        (response {:next-sync-url (next-sync-url (utils/get-api-root req) alias 123)})
        (response {:changes []
                   :next-sync-url (next-sync-url (utils/get-api-root req) alias 456)})))))

(defn endpoint [{:keys [akvo-flow-server-config] :as deps}]
  (-> (endpoint* deps)
      (wrap-resolve-alias akvo-flow-server-config)
      (jdo-pm/wrap-close-persistent-manager)))
