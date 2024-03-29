(ns org.akvo.flow-api.datastore.stats-test
  (:require [cheshire.core :as json]
            [clojure.test :refer [deftest is use-fixtures]]
            [org.akvo.flow-api.component.remote-api]
            [org.akvo.flow-api.datastore :as ds]
            [org.akvo.flow-api.datastore.stats :as stats]
            [org.akvo.flow-api.fixtures :as fixtures])
  (:import [com.google.appengine.api.datastore Entity DatastoreService DatastoreServiceFactory]
           [java.util Date]))

(def system {:components
             {:remote-api #'org.akvo.flow-api.component.remote-api/local-api}
             :dependencies {:remote-api []}
             :endpoints {}
             :config {}})

(use-fixtures :once (fixtures/system system))

(def values [[{"text" "two"}]
             [{"text" "ssdd" "code" "2"}
              {"text" "fsdfs" "code" "5"}
              {"text" "fbfb" "code" "OTHER" "isOther" :true}]
             [{"text" "two" "code" "2"}]])

(def values-no-code [[{"text" "two"}]
                     [{"text" "ssdd"}
                      {"text" "fsdfs"}
                      {"text" "fbfb" "code" "OTHER" "isOther" :true}]
                     [{"text" "two"}]])

(defn- new-question [^Long form-id ^Long question-id question-type now]
  (doto (Entity. "Question" question-id)
    (.setProperty "surveyId" form-id)
    (.setProperty "createdDateTime" now)
    (.setProperty "lastUpdateDateTime" now)
    (.setProperty "type" question-type)))

(defn- new-form-instance [^Long form-id ^Long form-instance-id now]
  (doto (Entity. "SurveyInstance" form-instance-id)
    (.setProperty "createdDateTime" now)
    (.setProperty "lastUpdateDateTime" now)
    (.setProperty "surveyId" form-id)))

(defn- new-answer [form-id form-instance-id question-id answer-type value now]
  (doto (Entity. "QuestionAnswerStore")
    (.setProperty "createdDateTime" now)
    (.setProperty "lastUpdateDateTime" now)
    (.setProperty "questionID" (str question-id))
    (.setProperty "surveyId" form-id)
    (.setProperty "surveyInstanceId" form-instance-id)
    (.setProperty "type" answer-type)
    (.setProperty "value" value)))

(defn- gen-option-question-test-data [^DatastoreService dss form-id question-id opt]
  (let [now (Date.)
        form-id (long form-id)
        question-id (long question-id)
        data [(new-question form-id question-id "OPTION" now)]
        data (apply conj
                    data
                    (flatten
                     (for [i (range 0 (count opt))]
                       (let [form-instance-id (System/currentTimeMillis)
                             form-instance (new-form-instance form-id form-instance-id now)
                             answer (new-answer form-id form-instance-id
                                                question-id "OPTION"
                                                (json/encode (nth opt i)) now)]
                         [form-instance answer]))))]
    (doseq [^Entity e data]
      (.put dss e))
    data))

(deftest test-option-question
  (ds/with-remote-api (:remote-api fixtures/*system*) "akvoflowsandbox"
    ;; without-code
    (let [dss (DatastoreServiceFactory/getDatastoreService)
          form-id (System/currentTimeMillis)
          question-id (System/currentTimeMillis)]
      (gen-option-question-test-data dss form-id question-id values-no-code)
      (is (= {"two" 2
              "ssdd" 1
              "fsdfs" 1
              "fbfb" 1}
             (stats/option-question-stats dss {:questionId (str question-id)
                                               :formId (str form-id)}))))
    ;; with-code
    (let [dss (DatastoreServiceFactory/getDatastoreService)
          form-id (System/currentTimeMillis)
          question-id (System/currentTimeMillis)]
      (gen-option-question-test-data dss form-id question-id values)
      (is (= {"2" 2
              "two" 1
              "5" 1
              "fbfb" 1}
             (stats/option-question-stats dss {:questionId (str question-id)
                                               :formId (str form-id)}))))))

(deftest test-number-question
  (ds/with-remote-api (:remote-api fixtures/*system*) "akvoflowsandbox"
    (let [dss (DatastoreServiceFactory/getDatastoreService)
          ;; After how many (full) strokes does water start flowing?
          results (stats/number-question-stats dss {:formId "145492013"
                                                    :questionId "146622024"})]
      (is
       (= {:max 365.0
           :min 0.0
           :sum 17373
           :count 129} (select-keys results [:max :min :sum :count])))
      (is
       (= (float (/ (:sum results) (:count results)))
          (float (:mean results)))))))

(deftest test-filter-anwers
  (ds/with-remote-api (:remote-api fixtures/*system*) "akvoflowsandbox"
    (let [dss (DatastoreServiceFactory/getDatastoreService)
          form-id (System/currentTimeMillis)
          form-instance-id (inc form-id)
          question-id (inc form-id)
          v (rand)
          now (Date.)]
      (doseq [e [(new-question form-id question-id "NUMBER" now)
                 (new-form-instance form-id form-instance-id now)
                 (new-answer form-id form-instance-id question-id "VALUE" (str v) now)
                 (new-answer form-id 0 question-id "VALUE" "1" now)]]
        (.put dss ^Entity e))
      (let [answers (stats/get-answers dss form-id question-id)
            answer (first answers)]
        (is (= 1 (count answers)))
        (is (= v answer))))))
