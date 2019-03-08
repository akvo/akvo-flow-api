(ns org.akvo.flow-api.boundary.folder
  (:refer-clojure :exclude [list])
  (:require org.akvo.flow-api.component.remote-api
            [org.akvo.flow-api.datastore :as ds]
            [org.akvo.flow-api.datastore.folder :as folder]))

(defn list [this instance-id user-id parent-id]
  (ds/with-remote-api this instance-id
    (doall (folder/list user-id parent-id))))
