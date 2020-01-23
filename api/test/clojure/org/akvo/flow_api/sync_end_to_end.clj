(ns org.akvo.flow-api.sync-end-to-end
  (:require [akvo.commons.psql-util :as pg]
            [cheshire.core :as json]
            [clj-http.client :as http]
            [clojure.java.io :as io]
            [clojure.java.jdbc :as jdbc]
            [clojure.test :refer :all]
            [duct.util.system :refer [load-system]]
            [org.akvo.flow-api.endpoint.sync :as endpoint]
            [org.akvo.flow-api.fixtures :as fixtures]
            [org.akvo.flow-api.unilog.unilog :refer [event-log-spec]]))

(def sync-url "http://localhost:3000/orgs/akvoflowsandbox/sync")

(def system (load-system (keep io/resource ["org/akvo/flow_api/system.edn" "dev.edn"])))

(use-fixtures :once (fixtures/system system))

(defn sync-endpoint [user params]
  (try
    (select-keys
     (http/get sync-url
               {:as :json
                :headers {"x-akvo-email" user
                          "content-type" "application/json"
                          "accept" "application/vnd.akvo.flow.v2+json"}
                :query-params params})
     [:status :body])
    (catch clojure.lang.ExceptionInfo e
      (select-keys (ex-data e) [:status :body]))))

(def event-log [{:entity {:id 149382279}
                 :eventType "formCreated"}
                {:entity {:id 147552013}
                 :eventType "formDeleted"}
                {:entity {:id 147502018}
                 :eventType "formUpdated"}
                {:entity {:id 144602050
                          :formId 145492013}
                 :eventType "formInstanceUpdated"}
                {:entity {:id 144602134
                          :formId 145492013}
                 :eventType "formInstanceDeleted"}])

(def more-changes [{:entity {:id 144622080
                             :formId 146532016}
                    :eventType "formInstanceUpdated"}])

(defn insert-log [db events]
  (jdbc/insert-multi! db :event_log (mapv (fn [evt]
                                            {:payload (pg/val->jsonb-pgobj evt)}) events)))

(defn delete-log [db]
  (jdbc/execute! db "DELETE FROM event_log")
  (jdbc/execute! db "ALTER SEQUENCE event_log_id_seq RESTART"))

(defn db-spec []
  (-> system
      :unilog-db
      :spec
      (assoc :db-name "u_akvoflowsandbox")
      event-log-spec))

(deftest check-sync-endpoint
  (let [user "akvo.flow.user.test@gmail.com"]
    (testing "Bad request on wrong parameters"
      (are [x y] (= x (:status y))
        400 (sync-endpoint user {})
        400 (sync-endpoint user {:initial false})
        400 (sync-endpoint user {:initial nil})
        400 (sync-endpoint user {:initial true
                                 :next true})
        400 (sync-endpoint user {:initial true
                                 :next true
                                 :cursor 0})
        400 (sync-endpoint user {:next true
                                 :cursor "wrong"})
        400 (sync-endpoint user {:next nil})
        400 (sync-endpoint user {:next true
                                 :cursor 400})))
    (testing "Sync URL with empty log"
      (let [db (db-spec)]
        (delete-log db)
        (is (= 200 (:status (sync-endpoint user {:initial true}))))))
    (testing "Sync URL with events"
      (let [db (db-spec)
            initial-url (sync-endpoint user {:initial true})]
        (is (= 200 (:status initial-url)))
        (insert-log db event-log)
        (let [{:keys [changes nextSyncUrl]} (-> (http/get (-> initial-url :body :nextSyncUrl)
                                                          {:as :json
                                                           :headers {"x-akvo-email" user
                                                                     "content-type" "application/json"
                                                                     "accept" "application/vnd.akvo.flow.v2+json"}})
                                                :body)]
          (is (= (:formDeleted changes) [147552013]))
          (is (= (:formInstanceDeleted changes) [144602134]))
          (is (= #{"144602050"}
                 (set (map :id (:formInstanceChanged changes)))))
          (is (= #{"149382279" "147502018"}
                 (set (map :id (:formChanged changes)))))
          (insert-log db more-changes)
          (let [{:keys [changes nextSyncUrl]} (-> (http/get nextSyncUrl
                                                            {:as :json
                                                             :headers {"x-akvo-email" user
                                                                       "content-type" "application/json"
                                                                       "accept" "application/vnd.akvo.flow.v2+json"}})
                                                  :body)]
            (is (= #{"144622080"}
                   (set (map :id (:formInstanceChanged changes)))))))))))