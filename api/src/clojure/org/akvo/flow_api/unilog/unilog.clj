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
            [cheshire.core :as json]))

(defn event-log-spec [config]
  (assert (not (empty? config)) "Config map is empty")
  (merge
    {:subprotocol "postgresql"
     :subname (format "//%s:%s/%s"
                (config :event-log-server)
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
                                  (assoc x ::form-instance-deleted (-> x :payload :entity :id)))))
                            (assoc x ::x ::invalid))))
                   ;(filter (comp #(s/valid? ::unilog-spec/eventType %) :eventType :payload))
                   ;(filter akvo-authorization.unilog.spec/valid?)
                   ;(take 100000)
                   )]
    (transduce
      pipeline
      (fn
        ([final]
         (let [form-instance-deleted (set (keep ::form-instance-deleted final))]
           {:unilog-id (:id (last final))
            :form-instance-deleted form-instance-deleted
            :form-instances (remove
                              (comp form-instance-deleted :id)
                              (distinct (keep ::form-instance-changed final)))}))
        ([sofar batch]
         (conj sofar batch)))
      []
      reducible)))

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