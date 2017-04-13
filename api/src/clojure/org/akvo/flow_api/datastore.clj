(ns org.akvo.flow-api.datastore
  (:require [clojure.java.io :as io]
            [org.akvo.flow-api.boundary.remote-api :as remote-api])
  (:import [com.gallatinsystems.framework.domain BaseDomain]
           [com.google.appengine.api.datastore Entity QueryResultIterator]
           [com.google.appengine.tools.remoteapi RemoteApiInstaller RemoteApiOptions]
           [java.text Format]
           [java.time.format DateTimeFormatter]
           [java.util Date]
           [java.util.logging LogManager]))

(set! *warn-on-reflection* true)

(.readConfiguration (LogManager/getLogManager)
                    (io/input-stream (io/resource "logging.properties")))

(defprotocol DomainObject
  (id [this])
  (created-at [this])
  (modified-at [this]))

(defmacro with-remote-api [remote-api instance-id & body]
  `(let [options# (remote-api/options ~remote-api ~instance-id)
         installer# (RemoteApiInstaller.)]
     (.install installer# options#)
     (try
       ~@body
       (finally
         (.uninstall installer#)))))

(def ^:private ^Format date-format
  (.toFormat (DateTimeFormatter/ISO_INSTANT)))

(defn to-iso-8601 [^java.util.Date date]
  (when date
    (.format date-format (.toInstant date))))

(extend-protocol DomainObject
  Entity
  (id [this]
    (-> this .getKey .getId))
  (created-at [this]
    (to-iso-8601 (.getProperty this "createdDateTime")))
  (modified-at [this]
    (to-iso-8601 (.getProperty this "lastUpdateDateTime")))

  BaseDomain
  (id [this]
    (-> this .getKey .getId))
  (created-at [this]
    (to-iso-8601 (.getCreatedDateTime this)))
  (modified-at [this]
    (to-iso-8601 (.getLastUpdateDateTime this))))

(def MAX_PAGE_SIZE 300)

(defn normalize-page-size [page-size]
  (if-not page-size
    30 ;; default
    (if (<= page-size MAX_PAGE_SIZE)
      page-size
      MAX_PAGE_SIZE)))

(defn cursor [^QueryResultIterator iterator]
  (let [cursor (.toWebSafeString (.getCursor iterator))]
    (when-not (empty? cursor)
      cursor)))
