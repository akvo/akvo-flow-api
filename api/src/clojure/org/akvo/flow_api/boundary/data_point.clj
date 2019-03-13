(ns org.akvo.flow-api.boundary.data-point
  (:refer-clojure :exclude [list])
  (:require org.akvo.flow-api.component.remote-api
            [org.akvo.flow-api.datastore :as ds]
            [org.akvo.flow-api.datastore.data-point :as data-point])
  (:import [com.google.appengine.api.datastore DatastoreServiceFactory]))

(defn list [remote-api instance-id user-id survey-definition opts]
  (ds/with-remote-api remote-api instance-id
    (let [ds (DatastoreServiceFactory/getDatastoreService)]
      (data-point/list ds survey-definition opts))))