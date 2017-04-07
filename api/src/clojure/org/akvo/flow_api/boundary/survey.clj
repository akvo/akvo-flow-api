(ns org.akvo.flow-api.boundary.survey
  (:refer-clojure :exclude [list])
  (:require org.akvo.flow-api.component.remote-api
            [org.akvo.flow-api.datastore :as ds]
            [org.akvo.flow-api.datastore.survey :as survey])
  (:import [org.akvo.flow_api.component.remote_api RemoteApi LocalApi]))

(defprotocol Survey
  (list [this instance-id user-id parent-id] "List surveys in a parent folder filtered for a particular user")
  (by-id [this instance-id user-id survey-id] "Get the survey definition"))

(extend-protocol Survey
  RemoteApi
  (list [this instance-id user-id folder-id]
    (ds/with-remote-api this instance-id
      (survey/list user-id folder-id)))
  (by-id [this instance-id user-id survey-id]
    (ds/with-remote-api this instance-id
      (survey/by-id user-id survey-id)))

  LocalApi
  (list [this instance-id user-id folder-id]
    (ds/with-remote-api this instance-id
      (survey/list user-id folder-id)))
  (by-id [this instance-id user-id survey-id]
    (ds/with-remote-api this instance-id
      (survey/by-id user-id survey-id))))
