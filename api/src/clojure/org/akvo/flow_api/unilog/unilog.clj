(ns org.akvo.flow-api.unilog.unilog
  (:import [com.google.appengine.api.datastore DatastoreServiceFactory])
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.string :as str]
            [org.akvo.flow-api.boundary.form-instance :as form-instance]
            [org.akvo.flow-api.unilog.spec :as unilog-spec]
            [org.akvo.flow-api.boundary.user :as user]
            [org.akvo.flow-api.datastore :as ds]
            [org.akvo.flow-api.boundary.survey :as survey]
            [org.akvo.flow-api.datastore.survey :as su]
            [org.akvo.flow-api.datastore.data-point :as data-point]
            [clojure.set :as set]
            [clojure.spec.alpha :as s]
            [cheshire.core :as json]
            [com.stuartsierra.component :as component]))

(defrecord UnilogConfig [spec]
  component/Lifecycle
  (start [this]
    (assoc this :spec spec))
  (stop [this]
    (dissoc this :spec)))

(defn unilog-config [spec]
  (->UnilogConfig spec))

(defn event-log-spec [config]
  (assert (not (empty? config)) "Config map is empty")
  (merge
    {:subprotocol "postgresql"
     :subname (format "//%s:%s/%s"
                (config :event-log-host)
                (config :event-log-port)
                (config :db-name))
     :ssl true
     :user (config :event-log-user)
     :password (config :event-log-password)}
    (:extra-jdbc-opts config)))

(def parse-json #(json/parse-string % true))

(defn form-data [{:keys [formId id surveyId]}]
  {:form-id formId
   :id id})

(defn form-instance-data [{:keys [formId formInstanceId]}]
  {:form-id formId
   :id formInstanceId})

(defn datapoint-data [{:keys [id surveyId]}]
  {:id id
   :survey-id surveyId})

(defn process-new-events [reducible]
  (let [pipeline (comp
                  (map (fn [x]
                          (try
                            (update x :payload parse-json)
                            (catch Exception e
                              x))))
                   (map (fn [x]
                          (if-not (unilog-spec/valid? x)
                            (assoc x ::x ::invalid)
                            (let [[k f]
                                  (case (-> x :payload :eventType)
                                    ("formInstanceUpdated" "formInstanceCreated") [::form-instance-changed form-data]
                                    ("answerUpdated" "answerCreated") [::form-instance-changed form-instance-data]
                                    "formInstanceDeleted" [::form-instance-deleted :id]
                                    ("formUpdated" "formCreated") [::form-changed :id]
                                    "formDeleted" [::form-deleted :id]
                                    ("dataPointUpdated" "dataPointCreated") [::data-point-changed datapoint-data]
                                    "dataPointDeleted" [::data-point-deleted :id]
                                    ("surveyGroupCreated" "surveyGroupUpdated") [::survey-changed :id]
                                    "surveyGroupDeleted" [::survey-deleted :id])]
                              (assoc x k (-> x :payload :entity f)))))))]
    (transduce
      pipeline
      (fn
        ([final]
         (let [form-deleted (set (keep ::form-deleted final))
               form-updated (apply disj (set (keep ::form-changed final)) form-deleted)
               form-instance-deleted (set (keep ::form-instance-deleted final))
               form-instances-grouped-by-form (group-by :form-id
                                                (remove
                                                  (comp form-deleted :form-id)
                                                  (remove
                                                    (comp form-instance-deleted :id)
                                                    (distinct (keep ::form-instance-changed final)))))
               data-point-deleted (set (keep ::data-point-deleted final))
               data-point-changed (remove #(data-point-deleted (:id %)) (set (keep ::data-point-changed final)))
               survey-deleted (set (keep ::survey-deleted final))
               survey-changed (apply disj (set (keep ::survey-changed final)) survey-deleted)]
           {::unilog-id (:id (last final))
            ::form-instance-deleted form-instance-deleted
            ::form-updated form-updated
            ::form-deleted form-deleted
            :forms-to-load (apply conj form-updated (keys form-instances-grouped-by-form))
            :surveys-to-load (apply conj survey-changed (map :survey-id data-point-changed))
            ::forms-instances-grouped-by-form form-instances-grouped-by-form
            ::data-point-changed data-point-changed
            ::data-point-deleted data-point-deleted
            ::survey-changed survey-changed
            ::survey-deleted survey-deleted}))
        ([sofar batch]
         (conj sofar batch)))
      []
      reducible)))

(defn filter-events-by-authorization [{::keys [forms-instances-grouped-by-form
                                               unilog-id
                                               form-updated
                                               form-deleted
                                               form-instance-deleted
                                               data-point-changed
                                               data-point-deleted
                                               survey-changed
                                               survey-deleted]}
                                      form-id->form
                                      survey-id->survey]
  {:unilog-id unilog-id
   :form-changed (->> form-updated (keep form-id->form) set)
   :form-deleted form-deleted
   :form-instance-deleted form-instance-deleted
   :form-instances-to-load (->> forms-instances-grouped-by-form
                                (keep (fn [[form-id form-instance]]
                                        (when-let [form (get form-id->form form-id)]
                                          {:form form
                                           :form-instance-ids (set (map :id form-instance))})))
                                set)
   :data-point-changed (->> data-point-changed
                            (filter (comp survey-id->survey :survey-id))
                            (map :id)
                            set)
   :data-point-deleted data-point-deleted
   :survey-changed (->> survey-changed (keep survey-id->survey) set)
   :survey-deleted survey-deleted})

(defn get-cursor [db]
  (let [result (first (jdbc/query db
                                  ["SELECT MAX(id) AS cursor FROM event_log"]))]
    (or (:cursor result) 0)))

(defn valid-offset? [offset db]
  (or (= offset 0)
      (let [result (first (jdbc/query db
                                      ["SELECT id AS offset FROM event_log WHERE id = ?" offset]))]
        (boolean (:offset result)))))

(defn read-event-log
  [db offset]
  (jdbc/reducible-query db
                        ["SELECT id, payload::text AS payload FROM event_log WHERE id > ? ORDER BY id ASC LIMIT 300" offset]
                        {:auto-commit? false :fetch-size 300}))

(defn process-unilog-events [offset db instance-id remote-api email]
  (ds/with-remote-api remote-api instance-id
    (let [ds (DatastoreServiceFactory/getDatastoreService)
          events (process-new-events (read-event-log db offset))
          user-id (user/id-by-email-or-throw-error* remote-api instance-id email)
          authorized-forms (set (map ds/id (su/list-forms-by-ids user-id (:forms-to-load events))))
          form-id->form (reduce (fn [acc form-id]
                                  (assoc acc form-id (su/get-form-definition (long form-id)
                                                                             {:include-survey-id? true})))
                                {}
                                authorized-forms)
          authorized-surveys (su/list-by-ids user-id (:surveys-to-load events))
          survey-id->survey (reduce (fn [acc survey]
                                      (assoc acc (ds/id survey) (su/survey->map survey)))
                                    {}
                                    authorized-surveys)
          authorized-events (filter-events-by-authorization events form-id->form survey-id->survey)
          form-instances (doall
                          (mapcat
                           (fn [to-load]
                             (form-instance/by-ids ds (:form to-load) (:form-instance-ids to-load)))
                           (:form-instances-to-load authorized-events)))
          data-point-changed (doall (data-point/by-ids ds (:data-point-changed authorized-events)))]
      (assoc authorized-events
             :form-instance-changed form-instances
             :data-point-changed data-point-changed))))

(defn get-db-name [instance-id]
  (str "u_" instance-id))

(defn wrap-db-connection [handler unilog-db]
  (fn [request]
    (let [db-name (get-db-name (:instance-id request))
          db-spec (-> unilog-db :spec (assoc :db-name db-name) event-log-spec)]
      (jdbc/with-db-connection [conn db-spec]
        (handler (assoc request :unilog-db-connection conn))))))
