(ns org.akvo.flow-api.boundary.survey
  (:refer-clojure :exclude [list])
  (:require [clojure.core.cache :as cache]
            org.akvo.flow-api.component.cache
            org.akvo.flow-api.component.remote-api
            [org.akvo.flow-api.datastore :as ds]
            [org.akvo.flow-api.datastore.survey :as survey]
            [org.akvo.flow-api.boundary.user :as user]))

(defn get-survey-definition [{:keys [cache]} instance-id user-id survey-id]
  (cache/lookup @cache [:survey-definitions instance-id user-id survey-id]))

(defn put-survey-definition [{:keys [cache]} instance-id user-id survey-id survey-definition]
  (swap! cache cache/miss [:survey-definitions instance-id user-id survey-id] survey-definition))

(defn list-by-folder [remote-api instance-id user-id folder-id]
  (ds/with-remote-api remote-api instance-id
    (doall (survey/list-by-folder user-id folder-id))))

(defn by-id [{:keys [survey-cache] :as this} instance-id user-id survey-id]
  (if-let [survey-definition (get-survey-definition survey-cache
                               instance-id
                               user-id
                               survey-id)]
    survey-definition
    (ds/with-remote-api this instance-id
      (let [survey-definition (survey/by-id user-id survey-id)]
        (put-survey-definition survey-cache
          instance-id
          user-id
          survey-id
          survey-definition)
        survey-definition))))

(defn filter-surveys
  "Given a list of surveys, returns the ones that the user has access to"
  [remote-api user-email surveys]
  (let [instances (into #{} (map :instance-id surveys))
        mapping-fn (if (> (count instances) 1) pmap map)]
    (survey/keep-allowed-to-see
      surveys
      (mapping-fn (fn [instance]
                    (when-let [user-id (user/id-by-email remote-api instance user-email)]
                      {:instance-id instance
                       :survey-ids (survey/cached-list-ids remote-api instance user-id)}))
        instances))))