(ns api.datastore.form-instance
  (:require[akvo.commons.gae :as gae]
           [akvo.commons.gae.query :as q]))

(def MAX_PAGE_SIZE 300)

(defn normalize-page-size [page-size]
  (if-not page-size
    30 ;; default
    (if (<= page-size MAX_PAGE_SIZE)
      page-size
      MAX_PAGE_SIZE)))

(defn form-instances-query [ds form-id cursor page-size]
  (.iterator (q/result ds
                       {:kind "SurveyInstance"
                        :filter (q/= "surveyId" form-id)}
                       {:start-cursor cursor
                        :chunk-size page-size
                        :limit page-size})))

(defn fetch-answers [ds form-instances]
  (reduce (fn [form-instance-answers form-instance-batch]
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
                                             (map (comp #(Long/parseLong %) :id)
                                                  form-instance-batch))}
                              {:chunk-size 300})))
          {}
          (partition-all 30 form-instances)))

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

(defn fetch-form-instances
  ([ds form-id]
   (fetch-form-instances ds form-id {}))
  ([ds form-id {:keys [page-size cursor]}]
   (let [page-size (normalize-page-size page-size)
         form-instances-iterator (form-instances-query ds form-id cursor page-size)
         cursor (.getCursor form-instances-iterator)
         form-instances (mapv form-instance-entity->map (iterator-seq form-instances-iterator))
         answers (fetch-answers ds form-instances)]
     {:form-instances (mapv (fn [form-instance]
                              (assoc form-instance
                                     :responses
                                     (get answers (:id form-instance))))
                            form-instances)
      :cursor cursor})))
