(ns org.akvo.flow-api.datastore.user
  (:require [org.akvo.flow-api.datastore :as ds])
  (:import [com.gallatinsystems.user.dao UserDao]))

(defn by-email [email]
  (.findUserByEmail (UserDao.) email))

(defn id [email]
  (when-let [user (by-email email)]
    (ds/id user)))
