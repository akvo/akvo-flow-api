(ns org.akvo.flow-api.datastore.folder
  (:import [com.gallatinsystems.user.dao UserDao]
           [org.akvo.flow.api.dao FolderDAO SurveyDAO])
  (:refer-clojure :exclude [list])
  (:require [org.akvo.flow-api.datastore :as ds]))

(defn list [user-id parent-id]
  (let [folder-dao (FolderDAO.)
        all-folders (.listAll folder-dao)
        user-folders (.filterByUserAuthorizationObjectId folder-dao
                                                         all-folders
                                                         user-id)]

    (->> user-folders
         (map (fn [folder]
                {:id (str (ds/id folder))
                 :name (.getName folder)
                 :parent-id (str (.getParentId folder))
                 :created-at (ds/created-at folder)
                 :modified-at (ds/modified-at folder)}))
         (filter #(= (:parent-id %) parent-id)))))
