(ns fetch-data.core
  (:require [akvo.commons.gae :as gae]
            [akvo.commons.gae.query :as q]
            [clojure.java.io :as io]))

(def flow-instance (System/getenv "FLOW_INSTANCE"))

(def ds-spec {:hostname (format "%s.appspot.com" flow-instance)
              :service-account-id (format "sa-%1$s@%1$s.iam.gserviceaccount.com" flow-instance)
              :private-key-file (format "/Users/jonasenlund/dev/akvo/akvo-flow-server-config/%1$s/%1$s.p12" flow-instance)
              :port 443})

(comment

  ;; Large one: 570000 QAs
  (def form-id 5021286)

  ;; Smaller one: 35000 QAs
  (def form-id 1060927)

  ;; Attempt 1: Pull all form instances and responses in one go: 40s
  (gae/with-datastore [ds ds-spec]
    (time
     (do
       (doall
        (q/result ds
                  {:kind "SurveyInstance"
                   :filter (q/= "surveyId" form-id)}
                  {:chunk-size 300}))
       (doall
        (q/result ds
                  {:kind "QuestionAnswerStore"
                   :filter (q/= "surveyId" form-id)}
                  {:chunk-size 300}))
       nil)))

  ;; Attempt 2: Fetch all form instances, then for each form instance fetch all answers: 374s
  (gae/with-datastore [ds ds-spec]
    (time
     (doseq [form-instance (q/result ds
                                     {:kind "SurveyInstance"
                                      :filter (q/= "surveyId" form-id)}
                                     {:chunk-size 300})]
       (println ".")
       (doall (q/result ds
                        {:kind "QuestionAnswerStore"
                         :filter (q/= "surveyInstanceId"
                                      (-> form-instance .getKey .getId))}
                        {:chunk-size 300})))))


  ;; Attempt 3: Fetch all form instances, partition by 30, and make an IN query
  ;; for answers: 183s
  (gae/with-datastore [ds ds-spec]
    (time
     (doseq [form-instance-batch (partition-all 30 (q/result ds
                                                             {:kind "SurveyInstance"
                                                              :filter (q/= "surveyId" form-id)}
                                                             {:chunk-size 300}))]
       (println ".")
       (doall (q/result ds
                        {:kind "QuestionAnswerStore"
                         :filter (q/in "surveyInstanceId"
                                       (map #(-> % .getKey .getId) form-instance-batch))}
                        {:chunk-size 300})))))

  )
