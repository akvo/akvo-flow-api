(ns org.akvo.flow-api.datastore.survey
  (:refer-clojure :exclude [list list*])
  (:require [clojure.core.cache :as cache]
            clojure.set
            [org.akvo.flow-api.anomaly :as anomaly]
            [org.akvo.flow-api.datastore :as ds])
  (:import [com.gallatinsystems.survey.domain SurveyGroup]
           [com.google.appengine.api.datastore DatastoreService Entity Key KeyFactory QueryResultIterator]
           [org.akvo.flow.api.dao FolderDAO SurveyDAO]
           [org.apache.commons.lang ArrayUtils]))

(defn list*
  [user-id]
  (let [survey-dao (SurveyDAO.)
        all-surveys (.listAll survey-dao)]
    (.filterByUserAuthorizationObjectId survey-dao all-surveys user-id)))

(defn list-by-ids
  [user-id survey-ids]
  (let [survey-dao (SurveyDAO.)
        all-surveys (.listByKeys survey-dao (ArrayUtils/toObject (long-array survey-ids)))]
    (.filterByUserAuthorizationObjectId survey-dao all-surveys user-id)))

(defn list-by-folder [user-id folder-id]
  (->>
    (list* user-id)
    (map (fn [survey]
           {:id (str (ds/id survey))
            :name (.getName survey)
            :folder-id (str (.getParentId survey))
            :created-at (ds/created-at survey)
            :modified-at (ds/modified-at survey)}))
    (filter #(= (:folder-id %) folder-id))))

(defn list-forms-by-ids
  [user-id form-ids]
  (let [form-dao (com.gallatinsystems.survey.dao.SurveyDAO.)
        all-forms (.listByKeys form-dao (ArrayUtils/toObject (long-array form-ids)))]
    (.filterByUserAuthorizationObjectId form-dao all-forms user-id)))

(defn ->question [question]
  (let [type* (str (.getType question))]
    (merge
     {:id (str (ds/id question))
      :name (.getText question)
      :type type*
      :order (.getOrder question)
      :variable-name (.getVariableName question)
      :personal-data (.getPersonalData question)
      :created-at (ds/created-at question)
      :modified-at (ds/modified-at question)}
     (when (= type* "CADDISFLY")
       {:caddisfly-resource-uuid (.getCaddisflyResourceUuid question)}))))

(defn question-group-definition [question-group]
  (let [questions (.values (.getQuestionMap question-group))
        qs (sort-by :order (map ->question questions))]
    {:id (str (ds/id question-group))
     :name (.getName question-group)
     :repeatable (boolean (.getRepeatable question-group))
     :questions qs
     :created-at (ds/created-at question-group)
     :modified-at (ds/modified-at question-group)}))

(defn get-form-definition
  ([form-id]
   (get-form-definition form-id {}))
  ([form-id {:keys [include-survey-id?]}]
   (let [form-dao (com.gallatinsystems.survey.dao.SurveyDAO.)
         ;; Includes question groups, but contrary to docstring does not contain questions
         form (.loadFullForm form-dao form-id)]
     (cond->
         {:id (str form-id)
          :name (.getName form)
          :question-groups (mapv (fn [question-group]
                                   (question-group-definition question-group))
                                 (.values (.getQuestionGroupMap form)))
          :version (.getVersion form)
          :created-at (ds/created-at form)
          :modified-at (ds/modified-at form)}
       include-survey-id? (assoc :survey-id (str (.getSurveyGroupId form)))))))

(defn by-id [user-id survey-id]
  (let [survey-dao (com.gallatinsystems.survey.dao.SurveyGroupDAO.)
        survey (if-let [survey (.getByKey survey-dao (Long/parseLong survey-id))]
                 survey
                 (anomaly/not-found "Survey not found"
                                    {:survey-id survey-id}))
        registration-form-id (.getNewLocaleSurveyId survey)
        form-dao (com.gallatinsystems.survey.dao.SurveyDAO.)
        all-forms (.listSurveysByGroup form-dao (Long/parseLong survey-id))
        forms (.filterByUserAuthorizationObjectId form-dao
                                                  all-forms
                                                  user-id)]
    (if (and (not-empty all-forms) (empty? forms))
      (anomaly/unauthorized "Not Authorized"
                            {:survey-id survey-id
                             :user-id user-id})
      {:id survey-id
       :name (.getName survey)
       :registration-form-id (str registration-form-id)
       :forms (mapv #(get-form-definition (ds/id %)) forms)
       :created-at (ds/created-at survey)
       :modified-at (ds/modified-at survey)})))

(defn keep-allowed-to-see [surveys-to-permission surveys-allowed-per-instance]
  (let [instance->survey-set (into {}
                               (map (fn [{:keys [instance-id survey-ids]}]
                                      [instance-id (set survey-ids)])
                                 surveys-allowed-per-instance))]
    (filter
      (fn [{:keys [instance-id survey-id]}]
        (contains? (get instance->survey-set instance-id) survey-id))
      surveys-to-permission)))

(defn survey->map
  [^SurveyGroup survey]
  {:id (str (ds/id survey))
   :name (.getName survey)
   :registration-form-id (str (.getNewLocaleSurveyId survey))
   :created-at (ds/created-at survey)
   :modified-at (ds/modified-at survey)})
