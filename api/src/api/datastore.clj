(ns api.datastore
  (:import [com.google.appengine.tools.remoteapi RemoteApiInstaller RemoteApiOptions]
           [java.time.format DateTimeFormatter]))

(defmacro with-remote-api [spec & body]
  `(let [host# (get ~spec :host)
         port# (get ~spec :port 443)
         iam-account# (get ~spec :iam-account)
         p12-path# (get ~spec :p12-path)
         remote-path# (let [trace-path# (get ~spec :trace-path)]
                        (if (nil? trace-path#)
                          "/remote_api"
                          (str "/traced_remote_api/" trace-path#)))
         options# (-> (RemoteApiOptions.)
                      (.server host# port#)
                      (.remoteApiPath remote-path#))]
     (.useServiceAccountCredential options#
                                   iam-account#
                                   p12-path#)
     (let [installer# (RemoteApiInstaller.)]
       (.install installer# options#)
       (try
         ~@body
         (finally
           (.uninstall installer#))))))

(defmacro with-local-api [& body]
  `(let [options# (-> (RemoteApiOptions.)
                      (.server "localhost" 8080))]
     (.useDevelopmentServerCredential options#)
     (let [installer# (RemoteApiInstaller.)]
       (.install installer# options#)
       (try
         ~@body
         (finally
           (.uninstall installer#))))))

(def ^:private date-format (.toFormat (DateTimeFormatter/ISO_INSTANT)))

(defn- to-iso-8601 [date]
  (.format date-format (.toInstant date)))

(defn created-at [entity]
  (to-iso-8601 (.getCreatedDateTime entity)))

(defn modified-at [entity]
  (to-iso-8601 (.getLastUpdateDateTime entity)))

(defn id [entity]
  (-> entity .getKey .getId))
