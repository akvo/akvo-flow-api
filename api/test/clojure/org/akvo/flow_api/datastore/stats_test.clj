(ns org.akvo.flow-api.datastore.stats-test
  (:require [cheshire.core :as json]
            [clojure.test :refer [deftest is use-fixtures]]
            [org.akvo.flow-api.component.remote-api]
            [org.akvo.flow-api.datastore :as ds]
            [org.akvo.flow-api.datastore.stats :as stats]
            [org.akvo.flow-api.fixtures :as fixtures])
  (:import [com.google.appengine.api.datastore Entity DatastoreService DatastoreServiceFactory]))

(def system {:components
             {:remote-api #'org.akvo.flow-api.component.remote-api/local-api}
             :dependencies {:remote-api []}
             :endpoints {}
             :config {}})

(use-fixtures :once (fixtures/system system))

(def values [[{"text" "two"}]
             [{"text" "ssdd" "code" "2"}
              {"text" "fsdfs" "code" "5"}
              {"text" "fbfb" "code" "OTHER" "isOther":true}]
             [{"text" "two" "code" "2"}]])


(defn- gen-test-data [^DatastoreService dss form-id question-id]
  (let [now (java.util.Date.)
        fid (long form-id)
        qid (long question-id)
        data [(doto (Entity. "Question" qid)
                   (.setProperty "surveyId" fid)
                   (.setProperty "createdDateTime" now)
                   (.setProperty "lastUpdateDateTime" now)
                   (.setProperty "type" "OPTION"))]
        data (apply conj
                    data
                   (flatten
                    (for [i (range 0 (count values))]
                      (let [form-instance-id (System/currentTimeMillis)
                            form-instance (doto (Entity. "SurveyInstance" form-instance-id)
                                            (.setProperty "createdDateTime" now)
                                            (.setProperty "lastUpdateDateTime" now)
                                            (.setProperty "surveyId" fid))
                            answer (doto (Entity. "QuestionAnswerStore")
                                     (.setProperty "createdDateTime" now)
                                     (.setProperty "lastUpdateDateTime" now)
                                     (.setProperty "questionID" (str question-id))
                                     (.setProperty "surveyId" fid)
                                     (.setProperty "surveyInstanceId" form-instance-id)
                                     (.setProperty "type" "OPTION")
                                     (.setProperty "value" (json/encode (nth values i))))]
                        [form-instance answer]))))]
    (doseq [^Entity e data]
      (.put dss e))
    data))

(deftest test-option-question
  (ds/with-remote-api (:remote-api fixtures/*system*) "akvoflowsandbox"
    (let [dss (DatastoreServiceFactory/getDatastoreService)
          form-id (System/currentTimeMillis)
          question-id (System/currentTimeMillis)]
     #_(stats/question-counts dss {:questionId "149362015"
                                       :formId "145492013"})
     (gen-test-data dss form-id question-id)
     (is (= {"two" 2
             "ssdd" 1
             "fsdfs" 1
             "fbfb" 1}
            (stats/question-counts dss {:questionId (str question-id)
                                        :formId (str form-id)}))))))
