(ns org.akvo.flow-api.datastore
  (:require [clojure.java.io :as io]
            [org.akvo.flow-api.boundary.remote-api :as remote-api])
  (:import [com.google.appengine.tools.remoteapi RemoteApiInstaller RemoteApiOptions]
           [java.time.format DateTimeFormatter]
           [java.util.logging LogManager]))

(.readConfiguration (LogManager/getLogManager)
                    (io/input-stream (io/resource "logging.properties")))

(defmacro with-remote-api [remote-api instance-id & body]
  `(let [options# (remote-api/options ~remote-api ~instance-id)
         installer# (RemoteApiInstaller.)]
     (.install installer# options#)
     (try
       ~@body
       (finally
         (.uninstall installer#)))))

(def ^:private date-format (.toFormat (DateTimeFormatter/ISO_INSTANT)))

(defn to-iso-8601 [date]
  (.format date-format (.toInstant date)))

(defn created-at [entity]
  (to-iso-8601 (.getCreatedDateTime entity)))

(defn modified-at [entity]
  (to-iso-8601 (.getLastUpdateDateTime entity)))

(defn id [entity]
  (-> entity .getKey .getId))
