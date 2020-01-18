(ns org.akvo.flow-api.sync
  (:require [clojure.test :refer :all]
            [org.akvo.flow-api.unilog.unilog :as unilog]
            [cheshire.core :as json]))

(def unilog-id-seq (atom 0))

(def any (partial rand-int 10000))

(defn form [form-id]
  {:id (swap! unilog-id-seq inc)
   :payload (json/generate-string {:eventType (rand-nth ["formUpdated" "formCreated"])
                                   :entity {:id form-id}})})

(defn form-deleted [form-id]
  {:id (swap! unilog-id-seq inc)
   :payload (json/generate-string {:eventType "formDeleted"
                                   :entity {:id form-id}})})

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

(defn can-see [& form-ids]
  (zipmap form-ids form-ids))

(defn changes-with-permissions
  ([events]
   (let [r (unilog/process-new-events-pure events)]
     (unilog/after-forms-loaded r (apply can-see (:forms-to-load r)))))
  ([events perms]
   (let [r (unilog/process-new-events-pure events)]
     (println r)
     (println (unilog/after-forms-loaded r perms))
     (unilog/after-forms-loaded r perms))))

(def form-instance-changes (comp :form-instances-to-load changes-with-permissions))
(def form-instance-deletes (comp :form-instance-deleted changes-with-permissions))
(def unilog-id (comp :unilog-id changes-with-permissions))
(def form-changes (comp :form-changes changes-with-permissions))
(def form-deletes (comp :form-deleted changes-with-permissions))

(deftest unilog-batch
  (testing "basic case"
    (is (= #{{:form 24 :form-instance-ids #{2 10}}}
          (form-instance-changes [(form-instance 24 2)
                                  (form-instance 24 10)])))

    (let [last-form-instance (form-instance (any) (any))]
      (is (= (:id last-form-instance)
            (unilog-id [(form-instance (any) (any))
                        (form-instance (any) (any))
                        last-form-instance])))))

  (testing "Multiple updates for the same form instance are grouped together"
    (is (= #{{:form 23 :form-instance-ids #{2}}}
          (form-instance-changes [(form-instance 23 2)
                                  (form-instance 23 2)]))))

  (testing "Answers"
    (is (= #{{:form 8 :form-instance-ids #{111}}}
          (form-instance-changes [(answer 8 111)]))))

  (testing "Answers and form instance updates are grouped together"
    (is (= #{{:form 8 :form-instance-ids #{111}}}
          (form-instance-changes [(answer 8 111)
                                  (form-instance 8 111)]))))

  (testing "Not permission for some forms"
    (is (= #{{:form {:any-form :definition} :form-instance-ids #{0}}}
          (form-instance-changes [(answer 1 0)
                                  (form-instance 2 (any))]
            {1 {:any-form :definition}}))))

  (testing "Delete form instance"
    (is (= #{10 11 112}
          (form-instance-deletes [(form-instance-deleted 11)
                                  (form-instance-deleted 10)
                                  (form-instance-deleted 112)])))

    (testing "Delete and update in same unilog batch"
      (is (empty?
            (form-instance-changes (shuffle [(answer (any) 10)
                                             (form-instance-deleted 10)]))))))

  (testing "form changes"
    (is (= #{{:some-form :definition}}
          (form-changes [(form 34)
                         (form 34)
                         (form 34)]
            {34 {:some-form :definition}})))

    (testing "permissions"
      (is (= #{34 36}
            (form-changes [(form 34)
                           (form 35)
                           (form 36)]
              (can-see 34 36)))))

    (testing "mix form and form-instances"
      (is (= #{1 10}
            (form-changes [(form 1)
                           (form-instance 1 (any))
                           (form-instance 2 (any))
                           (form 10)])))))

  (testing "form deletes"
    (is (= #{}
          (form-changes [(form 34)
                         (form-deleted 34)])))
    (testing "Delete and update in same unilog batch"
      (is (= #{34}
            (form-deletes [(form 34)
                           (form-deleted 34)]))))

    (testing "Delete of form and update of answer in the same unilog batch"
      (is (= #{}
            (form-instance-changes [(answer 44 (any))
                                    (form-deleted 44)])))))

  (testing "Not interesting event"
    (is (= 1010
          (unilog-id [{:id 1010
                       :payload (json/generate-string {:eventType "NonInteresting"})}]))))

  (testing "Invalid json"
    (is (= 1111
          (unilog-id [{:id 1111
                       :payload "notjson{"}])))))