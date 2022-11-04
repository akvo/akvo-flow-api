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

(defn- new-question [form-id question-id question-type now]
  (doto (Entity. "Question" question-id)
    (.setProperty "surveyId" form-id)
    (.setProperty "createdDateTime" now)
    (.setProperty "lastUpdateDateTime" now)
    (.setProperty "type" question-type)))

(defn- new-form-instance [form-id form-instance-id now]
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

(defn- gen-option-question-test-data [^DatastoreService dss form-id question-id]
  (let [now (Date.)
        form-id (long form-id)
        question-id (long question-id)
        data [(new-question form-id question-id "OPTION" now)]
        data (apply conj
                    data
                    (flatten
                     (for [i (range 0 (count values))]
                       (let [form-instance-id (System/currentTimeMillis)
                             form-instance (new-form-instance form-id form-instance-id now)
                             answer (new-answer form-id form-instance-id
                                                question-id "OPTION"
                                                (json/encode (nth values i)) now)]
                         [form-instance answer]))))]
    (doseq [^Entity e data]
      (.put dss e))
    data))

(deftest test-option-question
  (ds/with-remote-api (:remote-api fixtures/*system*) "akvoflowsandbox"
    (let [dss (DatastoreServiceFactory/getDatastoreService)
          form-id (System/currentTimeMillis)
          question-id (System/currentTimeMillis)]
      (gen-option-question-test-data dss form-id question-id)
      (is (= {"two" 2
              "ssdd" 1
              "fsdfs" 1
              "fbfb" 1}
             (stats/question-counts dss {:questionId (str question-id)
                                         :formId (str form-id)}))))))
