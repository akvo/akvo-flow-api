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

(defn changes [deps {:keys [alias instance-id params] :as req}]
  (let [{:keys [initial cursor next]} (spec/validate-params params-spec params)]
    (if (and initial (or cursor next))
      (anomaly/bad-request "Invalid parameters" {})
      (if (= "true" initial)
        (let [db {:connection (:unilog-db-connection req)}]
          (response {:next-sync-url (next-sync-url (utils/get-api-root req)
                                      alias
                                      (unilog/get-cursor db))}))
        (if (and next cursor)
          (let [db {:connection (:unilog-db-connection req)}
                offset (Long/parseLong cursor)]
            (if (unilog/valid-offset? offset db)
              (if (= offset (unilog/get-cursor db)) ;; end of the log
                (-> (status {} 204)
                    (header "Cache-Control" "max-age=60"))
                (let [changes (->
                               (unilog/process-unilog-events offset db instance-id (:remote-api deps))
                               (select-keys [:form-instance-changed
                                             :form-instance-deleted
                                             :form-changed
                                             :form-deleted
                                             :data-point-changed
                                             :data-point-deleted
                                             :unilog-id])
                               (update :form-deleted #(map str %))
                               (update :form-instance-deleted #(map str %))
                               (update :data-point-deleted #(map str %)))]
                  (-> (response {:changes (dissoc changes :unilog-id)
                                 :next-sync-url (next-sync-url (utils/get-api-root req) alias (:unilog-id changes))})
                      (header "Cache-Control" "no-cache"))))
              (anomaly/bad-request "Invalid cursor" {})))
          (anomaly/bad-request "Invalid parameters" {}))))))

(defn endpoint* [deps]
  (GET "/sync" req
    (#'changes deps req)))

(defn endpoint [{:keys [akvo-flow-server-config] :as deps}]
  (-> (endpoint* deps)
      (unilog/wrap-db-connection (:unilog-db deps))
      (wrap-resolve-alias akvo-flow-server-config)
      (jdo-pm/wrap-close-persistent-manager)))
