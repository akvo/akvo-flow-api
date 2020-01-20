(ns org.akvo.flow-api.unilog.unilog
  (:import [com.google.appengine.api.datastore DatastoreServiceFactory])
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.string :as str]
            org.akvo.flow-api.boundary.form-instance
            [org.akvo.flow-api.unilog.spec :as unilog-spec]
            [org.akvo.flow-api.boundary.user :as user]
            [org.akvo.flow-api.datastore :as ds]
            [org.akvo.flow-api.boundary.survey :as survey]
            [org.akvo.flow-api.datastore.survey :as su]
    ;[akvo-authorization.unilog.spec :as unilog-spec]
    ;       [akvo-authorization.unilog.message-processor]
    ;[jsonista.core :as json]
            [clojure.spec.alpha :as s]
            [hugsql.core :as hugsql]
    ;       [taoensso.timbre :as timbre]
    ;       [iapetos.core :as prometheus]
            [cheshire.core :as json]
            [com.stuartsierra.component :as component]))

(defrecord UnilogConfig [spec]
  component/Lifecycle
  (start [this]
    (assoc this :spec spec))
  (stop [this]
    (dissoc this :spec)))

(defn config [spec]
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

#_(defn unilog-dbs [db db-prefix]
    (->>
      (get-database-names db)
      (map :datname)
      (filter (fn [db-name] (str/starts-with? db-name db-prefix)))
      set))

(def parse-json #(json/parse-string % true))

(defn echo [{:keys [formId id]}]
  {:form-id formId
   :id id})

(defn echo2 [{:keys [formId formInstanceId]}]
  {:form-id formId
   :id formInstanceId})

(defn process-new-events-pure [reducible]
  (let [pipeline (comp
                   (map (fn [x]
                          (try
                            (update x :payload parse-json)
                            (catch Exception e
                              x))))
                   (map (fn [x]
                          (if (unilog-spec/valid? x)
                            (if ((comp #{"formInstanceUpdated" "formInstanceCreated"} :eventType :payload) x)
                              (assoc x ::form-instance-changed (-> x :payload :entity echo))
                              (if ((comp #{"answerUpdated" "answerCreated"} :eventType :payload) x)
                                (assoc x ::form-instance-changed (-> x :payload :entity echo2))
                                (if ((comp #{"formInstanceDeleted"} :eventType :payload) x)
                                  (assoc x ::form-instance-deleted (-> x :payload :entity :id))
                                  (if ((comp #{"formUpdated" "formCreated"} :eventType :payload) x)
                                    (assoc x ::form-changed (-> x :payload :entity :id))
                                    (if ((comp #{"formDeleted"} :eventType :payload) x)
                                      (assoc x ::form-deleted (-> x :payload :entity :id)))))))
                            (assoc x ::x ::invalid))))
                   ;(take 100000)
                   )]
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
                                                    (distinct (keep ::form-instance-changed final)))))]
           {::unilog-id (:id (last final))
            ::form-instance-deleted form-instance-deleted
            ::form-updated form-updated
            ::form-deleted form-deleted
            :forms-to-load (apply conj form-updated (keys form-instances-grouped-by-form))
            ::forms-instances-grouped-by-form form-instances-grouped-by-form}))
        ([sofar batch]
         (conj sofar batch)))
      []
      reducible)))

(defn after-forms-loaded [{::keys [forms-instances-grouped-by-form
                                   unilog-id
                                   form-updated
                                   form-deleted
                                   form-instance-deleted]}
                          form-id->form]
  {:unilog-id unilog-id
   :form-changes (->> form-updated
                   (keep form-id->form)
                   set)
   :form-deleted form-deleted
   :form-instance-deleted form-instance-deleted
   :form-instances-to-load (->> forms-instances-grouped-by-form
                             (keep (fn [[form-id form-instance]]
                                     (when-let [form (get form-id->form form-id)]
                                       {:form form
                                        :form-instance-ids (set (map :id form-instance))})))
                             set)})

(defn process-new-events [reducible]
  (let [last-unilog-id (atom nil)
        pipeline (comp
                   (map (fn [x] (update x :payload parse-json)))
                   (map (fn [x]
                          (reset! last-unilog-id {:unilog-id (:id x)})
                          x))
                   (filter unilog-spec/valid?)
                   ;(filter (comp #(s/valid? ::unilog-spec/eventType %) :eventType :payload))
                   ;(filter akvo-authorization.unilog.spec/valid?)
                   (take 100000)
                   )]
    (transduce
      pipeline
      (fn
        ([final]
         (println "starting" (count final))
         final)
        ([sofar batch]
         (conj sofar batch)))
      []
      reducible)))

(defn get-cursor [config]
  (let [result (first (jdbc/query (event-log-spec config)
                                  ["SELECT MAX(id) AS cursor FROM event_log"]))]
    (or (:cursor result) 0)))

#_(defn process-unilog-queue-for-tenant [{:keys [authz-db unilog-db] :as config} db-name]
    (let [offset (last-unilog-id authz-db db-name)]
      (process-new-events
        config
        db-name
        (jdbc/reducible-query
          (event-log-spec (assoc unilog-db :db-name db-name))
          ["SELECT id, payload::text FROM event_log WHERE id > ? ORDER BY id ASC " offset]
          {:auto-commit? false :fetch-size 1000}))))


(comment

  ;; HERE!!!
  (def unilog-db {:event-log-password ""
                  :event-log-user ""
                  :db-name "unilog"
                  :extra-jdbc-opts {:ssl false}
                  :event-log-port 5432
                  :event-log-server ""})


  (def x (let [offset 0]
           (process-new-events
             (jdbc/reducible-query
               (event-log-spec (assoc unilog-db :db-name (str "u_" instance-id)))
               ["SELECT id, payload::text FROM event_log WHERE id > ? ORDER BY id ASC" offset]
               {:auto-commit? false :fetch-size 3000}))))

  (count x)
  ; "dataPointUpdated" "answerCreated" "formInstanceCreated"
  ; "formUpdated" , "questionGroupUpdated", "questionDeleted"

  (keys (:remote-api reloaded.repl/system))

  (def instance-id "akvoflow-23")
  (def email "")                                            ;; HERE!!!!

  (def user-id (user/id-by-email-or-throw-error
                 (:remote-api reloaded.repl/system)
                 instance-id
                 email))


  (->> x
    (filter (comp #(clojure.string/includes? % "formInstanceDeleted") :eventType :payload))
    (map :payload)
    set
    (take 3)
    )

  (ds/with-remote-api (:remote-api reloaded.repl/system) instance-id
    (let [ds (DatastoreServiceFactory/getDatastoreService)]
      (->> x
        (filter (comp (partial = "answerCreated") :eventType :payload))
        (map :payload)
        (map (comp #(select-keys % [:formId :formInstanceId]) :entity))
        set
        (take 3)
        (group-by :formId)
        (mapv (fn [[formId forms-instances]]
                (println formId)
                (if-let [form-definition (su/get-form-definition (long formId))]
                  (org.akvo.flow-api.boundary.form-instance/by-ids
                    ds
                    form-definition
                    (map :formInstanceId forms-instances)
                    ))))
        (mapcat :form-instances)
        )))



  (ds/with-remote-api (:remote-api reloaded.repl/system) instance-id
    (let [ds (DatastoreServiceFactory/getDatastoreService)]
      (->> x
        (filter (comp (partial = "answerCreated") :eventType :payload))
        (map :payload)
        (map (comp #(select-keys % [:formId :formInstanceId]) :entity))
        set
        (take 3)
        (group-by :formId)
        (mapv (fn [[formId forms-instances]]
               (println formId)
               (if-let [form-definition (su/get-form-definition (long formId))]
                 (org.akvo.flow-api.boundary.form-instance/by-ids
                   ds
                   form-definition
                   (map :formInstanceId forms-instances)
                   ))))
        (mapcat :form-instances)
        )))


  (org.akvo.flow-api.boundary.data-point/by-ids
    (:remote-api reloaded.repl/system)
    "akvoflowsandbox"
    (->> x
      (filter (comp (partial = "dataPointUpdated") :eventType :payload))
      (map :payload)
      (map (comp :id :entity))
      set))

  (->> x
    (map (comp :eventType :payload))
    (frequencies))
  )
