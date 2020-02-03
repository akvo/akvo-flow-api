(ns org.akvo.flow-api.unilog.spec
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]))

(def id-seq (atom 1))
(s/def ::id (s/with-gen integer? #(gen/fmap (fn [_] (swap! id-seq inc)) (gen/return nil))))

(s/def ::formId ::id)
(s/def ::formInstanceId ::id)
(s/def ::dataPointId ::id)
(s/def ::surveyId ::id)
(s/def ::identifier string?)
(s/def ::name string?)
(s/def ::surveyGroupType #{"SURVEY"})

(s/def ::formInstance
  (s/keys :req-un [::id ::formId]))

(s/def ::form
  (s/keys :req-un [::id]))

(s/def ::dataPoint
  (s/keys :req-un [::id ::surveyId ::identifier]))

(s/def ::answer
  (s/keys :req-un [::formInstanceId ::formId]))

(s/def ::delete
  (s/keys :req-un [::id]))

(s/def ::survey
  (s/keys :req-un [::id ::name ::surveyGroupType]))

(def type-to-spec
  {"formInstanceDeleted" ::delete
   "formInstanceUpdated" ::formInstance
   "formInstanceCreated" ::formInstance
   "formUpdated" ::form
   "formCreated" ::form
   "formDeleted" ::delete
   "answerCreated" ::answer
   "answerUpdated" ::answer
   "dataPointCreated" ::dataPoint
   "dataPointUpdated" ::dataPoint
   "dataPointDeleted" ::delete
   "surveyGroupCreated" ::survey
   "surveyGroupUpdated" ::survey
   "surveyGroupDeleted" ::delete})

(s/def ::eventType (set (keys type-to-spec)))
(s/def ::entity map?)
(s/def ::payload (s/keys :req-un [::eventType ::entity]))
(s/def ::event (s/keys :req-un [::id ::payload]))


(defn valid? [m]
  (and
   (s/valid? ::event m)
   (when-let [spec (get type-to-spec (-> m :payload :eventType))]
     (s/valid?
      spec
      (-> m :payload :entity)))))
