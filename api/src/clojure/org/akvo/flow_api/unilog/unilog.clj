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

(defn form-data [{:keys [formId id]}]
  {:parent-id formId
   :id id})

(defn form-instance-data [{:keys [formId formInstanceId]}]
  {:parent-id formId
   :id formInstanceId})

(defn datapoint-data [{:keys [id surveyId]}]
  {:id id
   :parent-id surveyId})

(defn collapse [final {:keys [parent-delete-key parent-update-key child-delete-key child-update-key]}]
  (let [parent-deleted (set (keep parent-delete-key final))
        parent-updated (apply disj (set (keep parent-update-key final)) parent-deleted)
        child-deleted (set (keep child-delete-key final))
        child-changed (remove
                        (comp parent-deleted :parent-id)
                        (remove
                          (comp child-deleted :id)
                          (distinct (keep child-update-key final))))]
    {:parent-deleted parent-deleted
     :parent-updated parent-updated
     :child-deleted child-deleted
     :child-changed child-changed
     :parents-to-load (apply conj parent-updated (map :parent-id child-changed))}))

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
         (let [form-related (collapse final {:parent-delete-key ::form-deleted
                              :parent-update-key ::form-changed
                              :child-update-key ::form-instance-changed
                              :child-delete-key ::form-instance-deleted})

               survey-related (collapse final {:parent-delete-key ::survey-deleted
                                               :parent-update-key ::survey-changed
                                               :child-update-key ::data-point-changed
                                               :child-delete-key ::data-point-deleted})]
           {:forms-to-load (:parents-to-load form-related)
            :surveys-to-load (:parents-to-load survey-related)
            ::unilog-id (:id (last final))
            ::form-related form-related
            ::survey-related survey-related}))
        ([sofar batch]
         (conj sofar batch)))
      []
      reducible)))

(defn filter-events-by-authorization [{::keys [unilog-id
                                               form-related
                                               survey-related]}
                                      form-id->form
                                      survey-id->survey]
  {:unilog-id unilog-id

   :form-deleted (:parent-deleted form-related)
   :form-instance-deleted (:child-deleted form-related)

   :survey-deleted (:parent-deleted survey-related)
   :data-point-deleted (:child-deleted survey-related)

   :form-changed (->> (:parent-updated form-related) (keep form-id->form) set)
   :survey-changed (->> (:parent-updated survey-related) (keep survey-id->survey) set)

   :form-instances-to-load (->> (:child-changed form-related)
                             (group-by :parent-id)
                             (keep (fn [[form-id form-instance]]
                                     (when-let [form (get form-id->form form-id)]
                                       {:form form
                                        :form-instance-ids (set (map :id form-instance))})))
                             set)
   :data-point-changed (->> (:child-changed survey-related)
                         (filter (comp survey-id->survey :parent-id))
                         (map :id)
                         set)})

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
          user-id (user/id-by-email-or-throw-error remote-api instance-id email)
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
