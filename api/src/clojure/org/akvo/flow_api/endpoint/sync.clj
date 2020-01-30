(ns org.akvo.flow-api.endpoint.sync
  (:require [clojure.spec.alpha :as s]
            [compojure.core :refer [GET]]
            [org.akvo.flow-api.anomaly :as anomaly]
            [org.akvo.flow-api.endpoint.spec :as spec]
            [org.akvo.flow-api.endpoint.utils :as utils]
            [org.akvo.flow-api.middleware.jdo-persistent-manager :as jdo-pm]
            [org.akvo.flow-api.middleware.resolve-alias :refer [wrap-resolve-alias]]
            [org.akvo.flow-api.unilog.unilog :as unilog]
            [ring.util.response :refer [response status header]]))



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

(def no-more-changes (-> (status {} 204) (header "Cache-Control" "max-age=60")))

(defn initial-response
  [req]
  (let [alias (:alias req)
        api-root (utils/get-api-root req)
        db (:unilog-db-connection req)
        cursor (unilog/get-cursor db)]
    (response {:next-sync-url (next-sync-url api-root alias cursor)})))

(defn changes-response
  [offset db instance-id remote-api req]
  (let [alias (:alias req)
        api-root (utils/get-api-root req)
        changes (->
                 (unilog/process-unilog-events offset db instance-id remote-api)
                 (select-keys [:form-instance-changed
                               :form-instance-deleted
                               :form-changed
                               :form-deleted
                               :data-point-changed
                               :data-point-deleted
                               :unilog-id])
                 (update :form-deleted #(map str %))
                 (update :form-instance-deleted #(map str %))
                 (update :data-point-deleted #(map str %)))
        cursor (:unilog-id changes)]
    (-> (response {:changes (dissoc changes :unilog-id)
                   :next-sync-url (next-sync-url api-root alias cursor)})
        (header "Cache-Control" "no-cache"))))

(defn get-changes
  [req cursor instance-id remote-api]
  (let [db (:unilog-db-connection req)
        offset (Long/parseLong cursor)]
    (cond
      (not (unilog/valid-offset? offset db)) (anomaly/bad-request "Invalid cursor" {})
      (= offset (unilog/get-cursor db)) no-more-changes
      :else (changes-response offset db instance-id remote-api req))))

(defn changes [deps {:keys [alias instance-id params] :as req}]
  (let [{:keys [initial cursor next]} (spec/validate-params params-spec params)]
    (cond
      (and initial (or cursor next)) (anomaly/bad-request "Invalid parameters" {})
      (= "true" initial) (initial-response req)
      (and next cursor) (get-changes req cursor instance-id (:remote-api deps))
      :else (anomaly/bad-request "Invalid parameters" {}))))

(defn endpoint* [deps]
  (GET "/sync" req
    (#'changes deps req)))

(defn endpoint [{:keys [akvo-flow-server-config] :as deps}]
  (-> (endpoint* deps)
      (unilog/wrap-db-connection (:unilog-db deps))
      (wrap-resolve-alias akvo-flow-server-config)
      (jdo-pm/wrap-close-persistent-manager)))
