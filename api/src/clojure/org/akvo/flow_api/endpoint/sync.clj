(ns org.akvo.flow-api.endpoint.sync
  (:require [clojure.spec.alpha :as s]
            [compojure.core :refer [GET]]
            [org.akvo.flow-api.anomaly :as anomaly]
            [org.akvo.flow-api.endpoint.spec :as spec]
            [org.akvo.flow-api.endpoint.utils :as utils]
            [org.akvo.flow-api.middleware.jdo-persistent-manager :as jdo-pm]
            [org.akvo.flow-api.middleware.resolve-alias :refer [wrap-resolve-alias]]
            [org.akvo.flow-api.unilog.unilog :as unilog]
            [ring.util.response :refer [response]]))



(defn next-sync-url [api-root instance-id cursor]
  (utils/url-builder api-root instance-id "sync" {"next" true
                                                  "cursor" cursor}))

(defn get-db-name [instance-id]
  (str "u_" instance-id))

(s/def ::initial (s/and string? #(= "true" %)))
(s/def ::next (s/and string? #(= "true" %)))
(s/def ::cursor (s/and string? #(try (Long/parseLong %)
                                     (catch Exception _))))

;; TODO: Express parameter logic via spec?
(def params-spec (s/keys :opt-un [::initial ::next ::cursor]))

(defn changes [deps {:keys [alias instance-id params] :as req}]
  (let [{:keys [initial cursor next]} (spec/validate-params params-spec params)]
    (if (and initial cursor next)
      (anomaly/bad-request "Invalid parameters" {})
      (if (= "true" initial)
        (let [db-name (get-db-name instance-id)
              db-spec (-> deps :unilog-db :spec (assoc :db-name db-name))]
          (response {:next-sync-url (next-sync-url (utils/get-api-root req)
                                      alias
                                      (unilog/get-cursor db-spec))}))
        (if (and next cursor)
          (let [db-name (get-db-name instance-id)
                db-spec (-> deps :unilog-db :spec (assoc :db-name db-name))
                offset (Long/parseLong cursor)]
            (if (unilog/valid-offset? offset db-spec)
              (let [changes (unilog/process-unilog-events offset db-spec instance-id (:remote-api deps))]
                (response {:changes (select-keys changes [:form-instance-changed :form-instance-deleted :form-changed :form-deleted])
                           :next-sync-url (next-sync-url (utils/get-api-root req) alias (:unilog-id changes))}))
              (anomaly/bad-request "Invalid cursor" {})))
          (anomaly/bad-request "Invalid parameters" {}))))))

(defn endpoint* [deps]
  (GET "/sync" req
    (#'changes deps req)))

(defn endpoint [{:keys [akvo-flow-server-config] :as deps}]
  (-> (endpoint* deps)
      (wrap-resolve-alias akvo-flow-server-config)
      (jdo-pm/wrap-close-persistent-manager)))
