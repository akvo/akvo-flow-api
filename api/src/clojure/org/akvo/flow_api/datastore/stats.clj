(ns org.akvo.flow-api.datastore.stats
  (:require [akvo.commons.gae :as gae]
            [akvo.commons.gae.query :as q]
            [cheshire.core :as json]
            [org.akvo.flow-api.anomaly :as anomaly]
            [org.akvo.flow-api.datastore :as ds]
            [kixi.stats.core :as kixi]
            [redux.core :refer [fuse]])
  (:import [com.google.appengine.api.datastore DatastoreService Entity]))

(defn get-form-instance-ids [^DatastoreService dss ^Long formId]
  (into []
        (map ds/id)
        (ds/reducible-gae-query dss
                                {:kind "SurveyInstance"
                                 :filter (q/= "surveyId" formId)
                                 :keys-only? true}
                                {})))

(defn parse-value [v]
  (try
    (json/parse-string v true)
    (catch Exception _)))

(defn validate-question
  [^Entity question form-id question-id]
  (when (nil? question)
    (anomaly/not-found "Question not found" {}))
  (when (not= (.getProperty question "surveyId") form-id)
    (anomaly/bad-request (format "Question %s does not belong to form %s" question-id form-id) {}))
  (when (nil? (#{"OPTION" "NUMBER"} (.getProperty question "type")))
    (anomaly/bad-request "Not an [Option|Number] question" {})))

(defn get-answers
  [ds form-id question-id]
  (let [form-instance-ids (set (get-form-instance-ids ds form-id))]
    (flatten
     (into []
           (comp
            (filter #(form-instance-ids (.getProperty ^Entity % "surveyInstanceId")))
            (map #(parse-value (.getProperty ^Entity % "value"))))
           (ds/reducible-gae-query ds
                                   {:kind "QuestionAnswerStore"
                                    :filter (q/= "questionID" (str question-id))
                                    :sort-by "createdDateTime"
                                    ;; TODO:
                                    ;; Recover projections when index is up
                                    ;; https://github.com/akvo/akvo-flow/issues/3932
                                    #_#_:projections {"value" String
                                                  "surveyInstanceId" Long}}
                                   {})))))

(defn option-question-stats [ds {:keys [formId questionId]}]
  (let [question-id (Long/parseLong questionId)
        form-id (Long/parseLong formId)
        q (q/entity ds "Question" question-id)]
    (validate-question q form-id question-id)
    (->>
     (get-answers ds form-id question-id)
     (reduce (fn [acc {:keys [text code]}] ;; code vs text?
               (let [opt (if (and code (not= code "OTHER"))
                           code
                           text)]
               (if (contains? acc opt)
                 (update acc opt inc)
                 (assoc acc opt 1))))
             {}))))

(defn number-question-stats [ds {:keys [formId questionId]}]
  (let [question-id (Long/parseLong questionId)
        form-id (Long/parseLong formId)
        q (q/entity ds "Question" question-id)]
    (validate-question q form-id question-id)
    (->>
     (get-answers ds form-id question-id)
     (transduce identity (fuse {:sd kixi/standard-deviation
                                :max kixi/max
                                :min kixi/min
                                :mean kixi/mean
                                :sum +
                                :count kixi/count})))))

(comment
  (def ds-spec {:hostname "akvoflow-xx.appspot.com"
                :port 443
                :service-account-id "sa-akvoflow-xx@akvoflow-xx.iam.gserviceaccount.com"
                :private-key-file "/server-config/akvoflow-xx/akvoflow-xx.p12"})
  (gae/with-datastore [ds ds-spec]
    (doto (option-question-stats ds {:formId "313200912"
                                     :questionId "311160912"}) prn))
  )
