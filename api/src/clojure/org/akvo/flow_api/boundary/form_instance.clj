(ns org.akvo.flow-api.boundary.form-instance
  (:refer-clojure :exclude [list])
  (:require [org.akvo.flow-api.boundary.akvo-flow-server-config :refer [asset-url-root]]
            org.akvo.flow-api.component.remote-api
            [org.akvo.flow-api.datastore :as ds]
            [org.akvo.flow-api.datastore.form-instance :as form-instance])
  (:import [com.google.appengine.api.datastore DatastoreServiceFactory]
           [org.akvo.flow_api.component.remote_api RemoteApi LocalApi]))

(defprotocol FormInstance
  (list [this instance-id user-id form-definition opts] "List form instances for a given form definition"))

(extend-protocol FormInstance
  RemoteApi
  (list [{:keys [akvo-flow-server-config] :as this} instance-id user-id form-definition opts]
    (ds/with-remote-api this instance-id
      (let [ds (DatastoreServiceFactory/getDatastoreService)]
        (form-instance/list ds form-definition (assoc opts
                                                      :asset-url-root
                                                      (asset-url-root akvo-flow-server-config
                                                                      instance-id))))))

  LocalApi
  (list [this instance-id user-id form-definition opts]
    (ds/with-remote-api this instance-id
      (let [ds (DatastoreServiceFactory/getDatastoreService)]
        (form-instance/list ds form-definition (assoc opts
                                                      :asset-url-root
                                                      "https://localhost/images/"))))))
