(ns org.akvo.flow-api.boundary.survey
  (:refer-clojure :exclude [list])
  (:require [clojure.core.cache :as cache]
            org.akvo.flow-api.component.cache
            org.akvo.flow-api.component.remote-api
            [org.akvo.flow-api.datastore :as ds]
            [org.akvo.flow-api.datastore.survey :as survey])
  (:import [org.akvo.flow_api.component.cache TTLMemoryCache]
           [org.akvo.flow_api.component.remote_api RemoteApi LocalApi]))

(defprotocol ISurvey
  (list [this instance-id user-id parent-id] "List surveys in a parent folder filtered for a particular user")
  (by-id [this instance-id user-id survey-id] "Get the survey definition"))

(defprotocol ISurveyCache
  (lookup-survey-definition [cache instance-id user-id survey-id])
  (miss-survey-definition [cache instance-id user-id survey-id survey-definition]))

(extend-protocol ISurveyCache
  TTLMemoryCache
  (lookup-survey-definition [{:keys [cache]} instance-id user-id survey-id]
    (cache/lookup @cache [:survey-definitions instance-id user-id survey-id]))

  (miss-survey-definition [{:keys [cache]} instance-id user-id survey-id survey-definition]
    (swap! cache cache/miss [:survey-definitions instance-id user-id survey-id] survey-definition)))

(extend-protocol ISurvey
  RemoteApi
  (list [this instance-id user-id folder-id]
    (ds/with-remote-api this instance-id
      (survey/list user-id folder-id)))
  (by-id [{:keys [survey-cache] :as this} instance-id user-id survey-id]
    (if-let [survey-definition (lookup-survey-definition survey-cache
                                                         instance-id
                                                         user-id
                                                         survey-id)]
      survey-definition
      (ds/with-remote-api this instance-id
        (let [survey-definition (survey/by-id user-id survey-id)]
          (miss-survey-definition survey-cache
                                  instance-id
                                  user-id
                                  survey-id
                                  survey-definition)
          survey-definition))))

  LocalApi
  (list [this instance-id user-id folder-id]
    (ds/with-remote-api this instance-id
      (survey/list user-id folder-id)))
  (by-id [this instance-id user-id survey-id]
    (ds/with-remote-api this instance-id
      (survey/by-id user-id survey-id))))
