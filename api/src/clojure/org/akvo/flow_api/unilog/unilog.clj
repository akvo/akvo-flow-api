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

(defn survey-data [{:keys [id name]}]
  {:id id
   :name name})

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
                                    ("dataPointUpdated" "dataPointCreated") [::data-point-changed :id]
                                    "dataPointDeleted" [::data-point-deleted :id]
                                    ("surveyGroupCreated" "surveyGroupUpdated") [::survey-changed survey-data]
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
               data-point-changed (apply disj (set (keep ::data-point-changed final)) data-point-deleted)
               survey-deleted (set (keep ::survey-deleted final))
               survey-changed (remove #(survey-deleted (:id %)) (set (keep ::survey-changed final)))]
           {::unilog-id (:id (last final))
            ::form-instance-deleted form-instance-deleted
            ::form-updated form-updated
            ::form-deleted form-deleted
            :forms-to-load (apply conj form-updated (keys form-instances-grouped-by-form))
            ::forms-instances-grouped-by-form form-instances-grouped-by-form
            ::data-point-changed data-point-changed
            ::data-point-deleted data-point-deleted
            ::survey-changed survey-changed
            ::survey-deleted survey-deleted}))
        ([sofar batch]
         (conj sofar batch)))
      []
      reducible)))

(defn after-forms-loaded [{::keys [forms-instances-grouped-by-form
                                   unilog-id
                                   form-updated
                                   form-deleted
                                   form-instance-deleted
                                   data-point-changed
                                   data-point-deleted
                                   survey-changed
                                   survey-deleted]}
                          form-id->form]
  {:unilog-id unilog-id
   :form-changed (->> form-updated
                   (keep form-id->form)
                   set)
   :form-deleted form-deleted
   :form-instance-deleted form-instance-deleted
   :form-instances-to-load (->> forms-instances-grouped-by-form
                             (keep (fn [[form-id form-instance]]
                                     (when-let [form (get form-id->form form-id)]
                                       {:form form
                                        :form-instance-ids (set (map :id form-instance))})))
                             set)
   :data-point-changed data-point-changed
   :data-point-deleted data-point-deleted
   :survey-changed survey-changed
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

(defn process-unilog-events [offset db instance-id remote-api email]
  (ds/with-remote-api remote-api instance-id
    (let [ds (DatastoreServiceFactory/getDatastoreService)
          events (process-new-events
                   (jdbc/reducible-query db
                                         ["SELECT id, payload::text AS payload FROM event_log WHERE id > ? ORDER BY id ASC LIMIT 300" offset]
                                         {:auto-commit? false :fetch-size 300}))
          user-id (user/id-by-email-or-throw-error remote-api instance-id email)
          authorized-forms (set (map ds/id (su/list-forms user-id)))
          authorized-to-load (set/intersection authorized-forms (:forms-to-load events))
          form-id->form (reduce (fn [acc form-id]
                                  (assoc acc form-id (su/get-form-definition (long form-id)
                                                                             {:include-survey-id? true})))
                                {}
                                authorized-to-load)
          events-2 (after-forms-loaded events form-id->form) ;; TODO: get form definition from cache
          form-instances (doall
                           (mapcat
                             (fn [to-load]
                               (form-instance/by-ids ds (:form to-load) (:form-instance-ids to-load)))
                             (:form-instances-to-load events-2)))
          data-point-changed (doall (data-point/by-ids ds (:data-point-changed events-2)))]
      (assoc events-2 :form-instance-changed form-instances :data-point-changed data-point-changed))))

(defn get-db-name [instance-id]
  (str "u_" instance-id))

(defn wrap-db-connection [handler unilog-db]
  (fn [request]
    (let [db-name (get-db-name (:instance-id request))
          db-spec (-> unilog-db :spec (assoc :db-name db-name) event-log-spec)]
      (jdbc/with-db-connection [conn db-spec]
        (handler (assoc request :unilog-db-connection conn))))))
