(ns org.akvo.flow-api.endpoint.sync
  (:require [compojure.core :refer [GET]]
            [org.akvo.flow-api.anomaly :as anomaly]
            [org.akvo.flow-api.endpoint.utils :as utils]
            [org.akvo.flow-api.middleware.jdo-persistent-manager :as jdo-pm]
            [org.akvo.flow-api.middleware.resolve-alias :refer [wrap-resolve-alias]]
            [org.akvo.flow-api.unilog.unilog :as unilog]
            [ring.util.response :refer [response]]))


(defn next-sync-url [api-root instance-id cursor]
  (utils/url-builder api-root instance-id "sync" {"next" true
                                                  "cursor" cursor}))
(defn validate-params
  [params])

(defn get-db-name [instance-id]
  (str "u_" instance-id))

(defn endpoint* [deps]
  (GET "/sync" {:keys [alias query-params] :as req}
    (if (empty? query-params)
      (anomaly/bad-request "Missing required parameters" {})
      (if (= "true" (get query-params "initial"))
        (let [db-name (get-db-name (:instance-id req))
              db-spec (-> deps :unilog-db :spec (assoc :db-name db-name))]
          (response {:next-sync-url (next-sync-url (utils/get-api-root req)
                                                   alias
                                                   (unilog/get-cursor db-spec))}))
        (response {:changes []
                   :next-sync-url (next-sync-url (utils/get-api-root req) alias 456)})))))

(defn endpoint [{:keys [akvo-flow-server-config] :as deps}]
  (-> (endpoint* deps)
      (wrap-resolve-alias akvo-flow-server-config)
      (jdo-pm/wrap-close-persistent-manager)))
