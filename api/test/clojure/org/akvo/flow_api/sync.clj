(ns org.akvo.flow-api.sync
  (:require [clojure.test :refer :all]
            [org.akvo.flow-api.unilog.unilog :as unilog]
            [cheshire.core :as json]))

(def unilog-id-seq (atom 0))

(defn form-instance [form-id form-instance-id]
  {:id (swap! unilog-id-seq inc)
   :payload (json/generate-string {:eventType (rand-nth ["formInstanceUpdated" "formInstanceCreated"])
                                   :entity {:id form-instance-id
                                            :formId form-id}})})

(defn form-instance-deleted [form-instance-id]
  {:id (swap! unilog-id-seq inc)
   :payload (json/generate-string {:eventType "formInstanceDeleted"
                                   :entity {:id form-instance-id}})})

(defn answer [form-id form-instance-id]
  {:id (swap! unilog-id-seq inc)
   :payload (json/generate-string {:eventType (rand-nth ["answerCreated" "answerUpdated"])
                                   :entity {:formInstanceId form-instance-id
                                            :formId form-id}})})

(def unilog-id (comp :unilog-id unilog/process-new-events-pure))
(def form-instance-deletes (comp :form-instance-deleted unilog/process-new-events-pure))
(def form-instance-changes (comp set :form-instances unilog/process-new-events-pure))

(deftest unilog-batch
  (testing "basic case"
    (is (= #{{:form-id 24 :id 2} {:form-id 24 :id 10}}
          (form-instance-changes [(form-instance 24 2)
                                  (form-instance 24 10)])))

    (let [last-form-instance (form-instance 100 99)]
      (is (= (:id last-form-instance)
            (unilog-id [(form-instance 24 2)
                        (form-instance 24 3)
                        last-form-instance])))))

  (testing "Multiple updates for the same form instance are grouped together"
    (is (= #{{:form-id 23 :id 2}}
          (form-instance-changes [(form-instance 23 2)
                                  (form-instance 23 2)]))))

  (testing "Answers"
    (is (= #{{:form-id 8 :id 111}}
          (form-instance-changes [(answer 8 111)]))))

  (testing "Answers and form instance updates are grouped together"
    (is (= #{{:form-id 8 :id 111}}
          (form-instance-changes [(answer 8 111)
                                  (form-instance 8 111)]))))

  (testing "Delete form instance"
    (is (= #{10 11 112}
          (form-instance-deletes [(form-instance-deleted 11)
                                  (form-instance-deleted 10)
                                  (form-instance-deleted 112)])))

    (testing "Delete and update in same unilog batch"
      (is (empty?
            (form-instance-changes (shuffle [(answer 244 10)
                                             (form-instance-deleted 10)]))))))

  (testing "Not interesting event"
    (is (= 1010
          (unilog-id [{:id 1010
                       :payload (json/generate-string {:eventType "NonInteresting"})}]))))

  (testing "Invalid json"
    (is (= 1111
          (unilog-id [{:id 1111
                       :payload "notjson{"}])))))