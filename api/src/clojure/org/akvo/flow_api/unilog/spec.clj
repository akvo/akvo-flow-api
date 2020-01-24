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
   "dataPointDeleted" ::delete})

(s/def ::eventType (set (keys type-to-spec)))
(s/def ::entity map?)
(s/def ::payload (s/keys :req-un [::eventType ::entity]))
(s/def ::event (s/keys :req-un [::id ::payload]))


(defn valid? [m]
  #_(when-not (s/valid? ::event m)
    (s/explain ::event m))
  (and
    (s/valid? ::event m)
    (when-let [spec (get type-to-spec (-> m :payload :eventType))]
      (s/valid?
        spec
        (-> m :payload :entity)))))

(comment
  (gen/sample (s/gen ::event))

  (valid? {:id 1
           :payload {:eventType "answerCreated"
                     :entity {:formId 2
                              :formInstanceId 2}}}))
