(ns org.akvo.flow-api.datastore.survey-test
  (:require [clojure.test :refer :all]
            [org.akvo.flow-api.datastore :as ds]
            [org.akvo.flow-api.datastore.survey :as survey]
            [org.akvo.flow-api.datastore.user :as user]
            [org.akvo.flow-api.fixtures :as fixtures])
  (:import [com.google.appengine.api.datastore DatastoreServiceFactory]))

(def system {:components
             {:remote-api #'org.akvo.flow-api.component.remote-api/local-api}
             :dependencies {:remote-api []}
             :endpoints {}
             :config {}})

(use-fixtures :once (fixtures/system system))

(deftest survey-test
  (ds/with-remote-api (:remote-api fixtures/*system*) "akvoflowsandbox"
    (let [ds (DatastoreServiceFactory/getDatastoreService)
          user-1 (user/id "akvo.flow.user.test@gmail.com")
          user-2 (user/id "akvo.flow.user.test2@gmail.com")
          user-3 (user/id "akvo.flow.user.test3@gmail.com")
          folder-id "153142013"
          surveys-1 (survey/list user-1 folder-id)
          surveys-2 (survey/list user-2 folder-id)
          surveys-3 (survey/list user-3 folder-id)]
      (testing "Filtered surveys"
        (is (= 2 (count surveys-1)))
        (is (= 2 (count surveys-2)))
        (is (= 0 (count surveys-3)))))))

(deftest survey-definition-test
  (ds/with-remote-api (:remote-api fixtures/*system*) "akvoflowsandbox"
    (let [user-id (user/id "akvo.flow.user.test@gmail.com")
          survey-id "148412306"
          survey (survey/by-id user-id survey-id)
          form (first (:forms survey))
          question-group (first (:question-groups form))
          question (first (:questions question-group))]
      (are [x required-keys] (= (disj (set (keys x))
                                      :variable-name
                                      :modified-at
                                      :created-at)
                                required-keys)
        survey #{:id :name :forms :registration-form-id}
        form #{:id :name :question-groups}
        question-group #{:id :name :repeatable :questions}
        question #{:id :name :type :order}))))
