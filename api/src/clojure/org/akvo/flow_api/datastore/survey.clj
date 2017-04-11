(ns org.akvo.flow-api.datastore.survey
  (:refer-clojure :exclude [list])
  (:require [org.akvo.flow-api.anomaly :as anomaly]
            [org.akvo.flow-api.datastore :as ds])
  (:import [com.gallatinsystems.survey.dao.SurveyDAO]
           [org.akvo.flow.api.dao FolderDAO SurveyDAO]))

(defn list [user-id folder-id]
  (let [survey-dao (SurveyDAO.)
        all-surveys (.listAll survey-dao)
        user-surveys (.filterByUserAuthorizationObjectId survey-dao
                                                         all-surveys
                                                         user-id)]
    (->> user-surveys
         (map (fn [survey]
                {:id (str (ds/id survey))
                 :name (.getName survey)
                 :folder-id (str (.getParentId survey))
                 :created-at (ds/created-at survey)
                 :modified-at (ds/modified-at survey)}))
         (filter #(= (:folder-id %) folder-id)))))

(defn ->question [question]
  {:id (str (ds/id question))
   :name (.getText question)
   :type (str (.getType question))
   :order (.getOrder question)
   :created-at (ds/created-at question)
   :modified-at (ds/modified-at question)})

(defn question-group-definition [question-group questions]
  (let [qs (sort-by :order (map ->question questions))]
    {:id (str (ds/id question-group))
     :name (.getName question-group)
     :repeatable (boolean (.getRepeatable question-group))
     :questions qs
     :created-at (ds/created-at question-group)
     :modified-at (ds/modified-at question-group)}))

(defn get-form-definition [form-id]
  (let [form-dao (com.gallatinsystems.survey.dao.SurveyDAO.)
        ;; Includes question groups, but contrary to docstring does not contain questions
        form (.loadFullSurvey form-dao form-id)
        question-dao (com.gallatinsystems.survey.dao.QuestionDao.)
        questions (group-by #(.getQuestionGroupId %)
                            (.listQuestionsBySurvey question-dao form-id))]
    {:id (str (ds/id form))
     :name (.getName form)
     :question-groups (mapv (fn [question-group]
                              (question-group-definition question-group
                                                         (get questions (ds/id question-group))))
                            (.values (.getQuestionGroupMap form)))
     :created-at (ds/created-at form)
     :modified-at (ds/modified-at form)}))

(defn by-id [user-id survey-id]
  (let [survey-dao (com.gallatinsystems.survey.dao.SurveyGroupDAO.)
        survey (if-let [survey (.getByKey survey-dao (Long/parseLong survey-id))]
                 survey
                 (anomaly/not-found "Survey not found"
                                    {:survey-id survey-id}))
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
       :forms (mapv #(get-form-definition (ds/id %))
                    forms)
       :created-at (ds/created-at survey)
       :modified-at (ds/modified-at survey)})))
