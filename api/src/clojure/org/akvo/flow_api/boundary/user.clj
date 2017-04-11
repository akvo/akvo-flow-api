(ns org.akvo.flow-api.boundary.user
  (:require [clojure.core.cache :as cache]
            org.akvo.flow-api.component.cache
            org.akvo.flow-api.component.remote-api
            [org.akvo.flow-api.datastore :as ds]
            [org.akvo.flow-api.datastore.user :as user])
  (:import [org.akvo.flow_api.component.cache UserCache]
           [org.akvo.flow_api.component.remote_api RemoteApi LocalApi]))

(defprotocol IUser
  (id-by-email [this instance-id email] "Lookup the user id for a given email"))

(defprotocol IUserCache
  (lookup [this instance-id email] "Lookup a user id in a cache")
  (miss [this instance-id email id] "Call on cache miss"))

(extend-protocol IUserCache
  UserCache
  (lookup [{:keys [cache]} instance-id email]
    (cache/lookup @cache [instance-id email]))
  (miss [{:keys [cache]} instance-id email id]
    (swap! cache cache/miss [instance-id email] id)))


(extend-protocol IUser
  RemoteApi
  (id-by-email [{:keys [user-cache] :as this} instance-id email]
    (if-let [id (lookup user-cache instance-id email)]
      id
      (ds/with-remote-api this instance-id
        (let [id (user/id email)]
          (miss user-cache instance-id email id)
          id))))

  LocalApi
  (id-by-email [this instance-id email]
    (ds/with-remote-api this instance-id
      (user/id email))))
