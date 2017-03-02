(ns fetch-data.core
  (:require [akvo.commons.gae :as gae]
            [akvo.commons.gae.query :as q]
            [clojure.java.io :as io]))

(def flow-instance (System/getenv "FLOW_INSTANCE"))

(def ds-spec {:hostname (format "%s.appspot.com" flow-instance)
              :service-account-id (format "sa-%1$s@%1$s.iam.gserviceaccount.com" flow-instance)
              :private-key-file (format "/Users/jonasenlund/dev/akvo/akvo-flow-server-config/%1$s/%1$s.p12" flow-instance)
              :port 443})

;; Large one: 570000 QAs
;; (def form-id 5021286)

;; Smaller one: 35000 QAs
(def form-id 1060927)



(comment



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

  ;;; Pagination

  (def MAX_PAGE_SIZE 300)

  (defn form-instance-entity->map [form-instance]
    {:id (-> form-instance .getKey .getId str)
     :form-id (.getProperty form-instance "surveyId")
     :surveyal-time (.getProperty form-instance "surveyalTime")
     :submitter (.getProperty form-instance "submitterName")
     :submission-date (.getProperty form-instance "collectionDate")
     :device-identifier (.getProperty form-instance "deviceIdentifier")
     :data-point-id (.getProperty form-instance "surveyedLocaleId")
     :identifier (.getProperty form-instance "surveyedLocaleIdentifier")
     :display-name (.getProperty form-instance "surveyedLocaleDisplayName")})

  (defn do-fetch [page-size cursor]
    (gae/with-datastore [ds ds-spec]
      (let [page-size (if (<= page-size MAX_PAGE_SIZE)
                        page-size
                        MAX_PAGE_SIZE)
            form-instances-iterator (.iterator (q/result ds
                                                         {:kind "SurveyInstance"
                                                          :filter (q/= "surveyId" form-id)}
                                                         {:start-cursor cursor
                                                          :chunk-size page-size
                                                          :limit page-size}))
            form-instances-seq (mapv form-instance-entity->map (iterator-seq form-instances-iterator))
            cursor (.getCursor form-instances-iterator)
            answers (reduce (fn [form-instance-answers form-instance-batch]
                              (reduce (fn [fia answer]
                                        (assoc-in fia
                                                  [(str (.getProperty answer "surveyInstanceId"))
                                                   (.getProperty answer "questionID")
                                                   (or (.getProperty answer "iteration") 0)]
                                                  (or (.getProperty answer "value")
                                                      (.getProperty answer "valueText"))))
                                      form-instance-answers
                                      (q/result ds
                                                {:kind "QuestionAnswerStore"
                                                 :filter (q/in "surveyInstanceId"
                                                               (map (comp #(Long/parseLong %) :id) form-instance-batch))}
                                                {:chunk-size 300})))
                            {}
                            (partition-all 30 form-instances-seq))]
        {:form-instances (mapv (fn [form-instance]
                                 (assoc form-instance
                                        :responses
                                        (get answers (:id form-instance))))
                               form-instances-seq)
         :cursor cursor})))


  (def res1 (do-fetch 2 nil)) res1
  (def res2 (do-fetch 2 (:cursor res1)))

  res1
  res2

  )
