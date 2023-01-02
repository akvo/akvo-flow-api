(ns org.akvo.flow-api.datastore.folder
  (:refer-clojure :exclude [list])
  (:require [org.akvo.flow-api.datastore :as ds])
  (:import [com.gallatinsystems.survey.domain SurveyGroup]
           [org.akvo.flow.api.dao FolderDAO]))

(defn list [user-id parent-id]
  (let [folder-dao (FolderDAO.)
        all-folders (.listAll folder-dao)
        user-folders (.filterByUserAuthorizationObjectId folder-dao
                                                         all-folders
                                                         user-id)]

    (->> user-folders
         (map (fn [^SurveyGroup folder]
                {:id (str (ds/id folder))
                 :name (.getName folder)
                 :parent-id (str (.getParentId folder))
                 :created-at (ds/created-at folder)
                 :modified-at (ds/modified-at folder)}))
         (filter #(= (:parent-id %) parent-id)))))
