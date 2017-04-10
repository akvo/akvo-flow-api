(ns org.akvo.flow-api.datastore.user
  (:import [com.gallatinsystems.user.dao UserDao])
  (:require [org.akvo.flow-api.datastore :as ds]))

(defn by-email [email]
  (if-let [user (.findUserByEmail (UserDao.) email)]
    user
    (throw (ex-info (format "User %s does not exist" email)
                    {:status :unauthorized
                     :email email}))))

(defn id [email]
  (ds/id (by-email email)))
