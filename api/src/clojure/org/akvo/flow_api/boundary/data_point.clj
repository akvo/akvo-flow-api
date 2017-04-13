(ns org.akvo.flow-api.boundary.data-point
  (:refer-clojure :exclude [list])
  (:require org.akvo.flow-api.component.remote-api
            [org.akvo.flow-api.datastore :as ds]
            [org.akvo.flow-api.datastore.data-point :as data-point])
  (:import [com.google.appengine.api.datastore DatastoreServiceFactory]
           [org.akvo.flow_api.component.remote_api RemoteApi LocalApi]))

(defprotocol DataPoint
  (list [this instance-id user-id survey-definition opts] "List data points for a given survey definition"))

(extend-protocol DataPoint
  RemoteApi
  (list [this instance-id user-id survey-definition opts]
    (ds/with-remote-api this instance-id
      (let [ds (DatastoreServiceFactory/getDatastoreService)]
        (data-point/list ds survey-definition opts))))

  LocalApi
  (list [this instance-id user-id survey-definition opts]
    (ds/with-remote-api this instance-id
      (let [ds (DatastoreServiceFactory/getDatastoreService)]
        (data-point/list ds survey-definition opts)))))
