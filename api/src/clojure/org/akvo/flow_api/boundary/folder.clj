(ns org.akvo.flow-api.boundary.folder
  (:refer-clojure :exclude [list])
  (:require org.akvo.flow-api.component.remote-api
            [org.akvo.flow-api.datastore.folder :as folder]))

(defn list [user-id parent-id]
  (doall (folder/list user-id parent-id)))
