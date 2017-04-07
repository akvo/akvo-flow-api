(ns org.akvo.flow-api.boundary.folder
  (:refer-clojure :exclude [list])
  (:require org.akvo.flow-api.component.remote-api
            [org.akvo.flow-api.datastore :as ds]
            [org.akvo.flow-api.datastore.folder :as folder])
  (:import [org.akvo.flow_api.component.remote_api RemoteApi LocalApi]))

(defprotocol Folder
  (list [this instance-id user-id parent-id] "List child folders to a parent folder filtered for a particular user"))

(extend-protocol Folder
  RemoteApi
  (list [this instance-id user-id parent-id]
    (ds/with-remote-api this instance-id
      (folder/list user-id parent-id)))

  LocalApi
  (list [this instance-id user-id parent-id]
    (ds/with-remote-api this instance-id
      (folder/list user-id parent-id))))
