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

(defn form-instances-query [ds form-definition {:keys [cursor page-size]}]
  (let [page-size (normalize-page-size page-size)]
    (.iterator (q/result ds
                         {:kind "SurveyInstance"
                          :filter (q/= "surveyId" (Long/parseLong (:id form-definition)))}
                         {:start-cursor cursor
                          :chunk-size page-size
                          :limit page-size}))))

(defn question-type-map
  "Builds a map from question id to type"
  [form-definition]
  (reduce (fn [question-types {:keys [id type]}]
            (assoc question-types id type))
          {}
          (mapcat :questions (:question-groups form-definition))))

(defmulti parse-response (fn [type response-str] type))

(defn fetch-answers [ds form-definition form-instances]
  (let [question-types (question-type-map form-definition)]
    (reduce (fn [form-instance-answers form-instance-batch]
              (reduce (fn [fia answer]
                        (let [form-instance-id (str (.getProperty answer "surveyInstanceId"))
                              question-id (.getProperty answer "questionID")
                              response-str (or (.getProperty answer "value")
                                               (.getProperty answer "valueText"))
                              iteration (or (.getProperty answer "iteration") 0)
                              response (when response-str
                                         (parse-response (get question-types question-id)
                                                         response-str))]
                          (assoc-in fia
                                    [form-instance-id
                                     question-id
                                     iteration]
                                    response)))
                      form-instance-answers
                      (q/result ds
                                {:kind "QuestionAnswerStore"
                                 :filter (q/in "surveyInstanceId"
                                               (map (comp #(Long/parseLong %) :id)
                                                    form-instance-batch))}
                                {:chunk-size 300})))
            {}
            (partition-all 30 form-instances))))

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
  ([ds form-definition]
   (fetch-form-instances ds form-definition {}))
  ([ds form-definition opts]
   (let [form-instances-iterator (form-instances-query ds form-definition opts)
         cursor (.getCursor form-instances-iterator)
         form-instances (mapv form-instance-entity->map (iterator-seq form-instances-iterator))
         answers (fetch-answers ds form-definition form-instances)]
     {:form-instances (mapv (fn [form-instance]
                              (assoc form-instance
                                     :responses
                                     (get answers (:id form-instance))))
                            form-instances)
      :cursor cursor})))
