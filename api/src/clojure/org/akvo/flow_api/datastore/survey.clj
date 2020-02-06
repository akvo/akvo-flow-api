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
  ([user-id survey-ids]
   (let [survey-dao (SurveyDAO.)
         all-surveys (if survey-ids
                       (.listByKeys survey-dao (ArrayUtils/toObject (long-array survey-ids)))
                       (.listAll survey-dao))]
     (.filterByUserAuthorizationObjectId survey-dao all-surveys user-id)))
  ([user-id]
   (list* user-id nil)))

(defn list-ids [user-id]
  (->>
    (list* user-id)
    (map (fn [survey]
           (str (ds/id survey))))))

(defn cached-list-ids [{:keys [survey-list-cache] :as remote-api} instance user-id]
  (if-let [survey-list (cache/lookup @(:cache survey-list-cache) [instance user-id])]
    survey-list
    (ds/with-remote-api remote-api instance
      (let [survey-list (doall (list-ids user-id))]
        (swap! (:cache survey-list-cache) cache/miss [instance user-id] survey-list)
        survey-list))))

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

(defn list-forms
  ([user-id]
   (list-forms user-id nil))
  ([user-id form-ids]
   (let [form-dao (com.gallatinsystems.survey.dao.SurveyDAO.)
         all-forms (if form-ids
                     (.listByKeys form-dao (ArrayUtils/toObject (long-array form-ids)))
                     (.list form-dao "all"))]
     (.filterByUserAuthorizationObjectId form-dao all-forms user-id))))

(defn ->question [question]
  (let [type* (str (.getType question))]
    (merge
     {:id (str (ds/id question))
      :name (.getText question)
      :type type*
      :order (.getOrder question)
      :variable-name (.getVariableName question)
      :created-at (ds/created-at question)
      :modified-at (ds/modified-at question)}
     (when (= type* "CADDISFLY")
       {:caddisfly-resource-uuid (.getCaddisflyResourceUuid question)}))))

(defn question-group-definition [question-group questions]
  (let [qs (sort-by :order (map ->question questions))]
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
         form (.loadFullSurvey form-dao form-id)
         question-dao (com.gallatinsystems.survey.dao.QuestionDao.)
         questions (group-by #(.getQuestionGroupId %)
                             (.listQuestionsBySurvey question-dao form-id))]
     (cond->
         {:id (str form-id)
          :name (.getName form)
          :question-groups (mapv (fn [question-group]
                                   (question-group-definition question-group
                                                              (get questions (ds/id question-group))))
                                 (.values (.getQuestionGroupMap form)))
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
