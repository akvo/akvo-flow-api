(ns api.datastore.user
  (:import [com.gallatinsystems.user.dao UserDao])
  (:require [api.datastore :as ds]))

(defn by-email [email]
  (if-let [user (.findUserByEmail (UserDao.) email)]
    user
    (throw (ex-info (format "User %s does not exist" email)
                    {:email email}))))

(defn id [email]
  (ds/id (by-email email)))
