(ns org.akvo.flow-api.boundary.form-instance
  (:refer-clojure :exclude [list])
  (:require [org.akvo.flow-api.boundary.akvo-flow-server-config :refer [asset-url-root]]
            org.akvo.flow-api.component.remote-api
            [org.akvo.flow-api.datastore.form-instance :as form-instance])
  (:import [com.google.appengine.api.datastore DatastoreServiceFactory]))

(defn list [{:keys [akvo-flow-server-config]} instance-id _user-id form-definition opts]
  (let [ds (DatastoreServiceFactory/getDatastoreService)]
    (form-instance/list ds form-definition (assoc opts
                                                  :asset-url-root
                                                  (asset-url-root akvo-flow-server-config
                                                                  instance-id)))))

(defn by-ids [ds form-definition form-instance-ids]
  (form-instance/by-ids ds form-definition form-instance-ids))
