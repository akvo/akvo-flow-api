(ns org.akvo.flow-api.datastore.stats
  (:require [akvo.commons.gae :as gae]
            [akvo.commons.gae.query :as q]
            [cheshire.core :as json]
            [org.akvo.flow-api.anomaly :as anomaly]
            [org.akvo.flow-api.datastore :as ds])
  (:import [com.google.appengine.api.datastore DatastoreService Entity QueryResultIterable]))

(defn get-form-instance-ids [^DatastoreService ds ^Long formId]
  (map ds/id
   (iterator-seq
    (.iterator
     ^QueryResultIterable
     (q/result ds {:kind "SurveyInstance"
                   :filter (q/= "surveyId" formId)
                   :keys-only? true})))))

(defn parse-value [v]
  (try
    (json/parse-string v true)
    (catch Exception e
      (.getMessage e))))

(defn get-answers [ds formInstanceId questionId]
  (iterator-seq
   (.iterator
    ^QueryResultIterable
    (q/result ds {:kind "QuestionAnswerStore"
                  :filter (q/and
                           (q/= "questionID" (str questionId))
                           (q/= "surveyInstanceId" formInstanceId))}))))

(defn get-answer [ds questionId]
  (q/entity ds "QuestionAnswerStore" questionId))

(defn validate-question
  [^Entity question form-id question-id]
  (when (nil? question)
    (anomaly/not-found "Question not found" {}))
  (when (not= (.getProperty question "surveyId") form-id)
    (anomaly/bad-request (format "Question %s does not belong to form %s" question-id form-id) {}))
  (when (not= (.getProperty question "type") "OPTION")
    (anomaly/bad-request "Not an OPTION question" {})))

(defn question-counts [ds {:keys [formId questionId]}]
  (let [question-id (Long/parseLong questionId)
        form-id (Long/parseLong formId)
        q (q/entity ds "Question" question-id)]
    (validate-question q form-id question-id)
    (reduce (fn [acc i]
              (let [t (:text i)]
                (if (contains? acc t)
                  (update acc t inc)
                  (assoc acc t 1))))
            {}
            (flatten
             (reduce (fn [acc formInstanceId]
                       (let [answers (get-answers ds formInstanceId question-id)]
                         (conj acc
                               (map #(parse-value (.getProperty ^Entity % "value")) answers))))
                     []
                     (get-form-instance-ids ds form-id))))))


(comment
  (def ds-spec {:hostname "akvoflow-xx.appspot.com"
                :port 443
                :service-account-id "sa-akvoflow-xx@akvoflow-xx.iam.gserviceaccount.com"
                :private-key-file "/server-config/akvoflow-xx/akvoflow-xx.p12"})
  (gae/with-datastore [ds ds-spec]
    (doto (question-counts ds {:formId "313200912"
                               :questionId "311160912"}) prn))
  )
