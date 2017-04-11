(ns org.akvo.flow-api.datastore.user
  (:require [org.akvo.flow-api.anomaly :as anomaly]
            [org.akvo.flow-api.datastore :as ds])
  (:import [com.gallatinsystems.user.dao UserDao]))

(defn by-email [email]
  (if-let [user (.findUserByEmail (UserDao.) email)]
    user
    (anomaly/unauthorized "User does not exist"
                          {:email email})))

(defn id [email]
  (ds/id (by-email email)))
