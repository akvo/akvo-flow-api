(ns org.akvo.flow-api.boundary.user
  (:require org.akvo.flow-api.component.remote-api
            [org.akvo.flow-api.datastore :as ds]
            [org.akvo.flow-api.datastore.user :as user])
  (:import [org.akvo.flow_api.component.remote_api RemoteApi LocalApi]))

(defprotocol User
  (id-by-email [this instance-id email] "Lookup the user id for a given email"))

(extend-protocol User
  RemoteApi
  (id-by-email [this instance-id email]
    (ds/with-remote-api this instance-id
      (user/id email)))

  LocalApi
  (id-by-email [this instance-id email]
    (ds/with-remote-api this instance-id
      (user/id email))))
