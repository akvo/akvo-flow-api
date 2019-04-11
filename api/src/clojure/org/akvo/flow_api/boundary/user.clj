(ns org.akvo.flow-api.boundary.user
  (:require [clojure.core.cache :as cache]
            org.akvo.flow-api.component.cache
            org.akvo.flow-api.component.remote-api
            [org.akvo.flow-api.datastore :as ds]
            [org.akvo.flow-api.datastore.user :as user]
            [org.akvo.flow-api.anomaly :as anomaly]))

(defn get-id [{:keys [cache]} instance-id email]
  (cache/lookup @cache [instance-id email]))

(defn put-id [{:keys [cache]} instance-id email id]
  (swap! cache cache/miss [instance-id email] id))

(defn id-by-email [{:keys [user-cache] :as this} instance-id email]
  (if-let [id (get-id user-cache instance-id email)]
    id
    (ds/with-remote-api this instance-id
      (let [id (user/id email)]
        (put-id user-cache instance-id email id)
        id))))

(defn id-by-email-or-throw-error [remote-api instance-id email]
  (or
    (id-by-email remote-api instance-id email)
    (anomaly/unauthorized "User does not exist" {:email email})))