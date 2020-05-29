(ns org.akvo.flow-api.datastore.survey-test
  (:require [clojure.test :refer :all]
            [org.akvo.flow-api.datastore :as ds]
            [org.akvo.flow-api.datastore.survey :as survey]
            [org.akvo.flow-api.datastore.user :as user]
            [org.akvo.flow-api.fixtures :as fixtures]))

(def system {:components
             {:remote-api #'org.akvo.flow-api.component.remote-api/local-api}
             :dependencies {:remote-api []}
             :endpoints {}
             :config {}})

(use-fixtures :once (fixtures/system system))

(defn surveys-for-user [user]
  (count (survey/list-by-folder (user/id user) "153142013")))

(deftest survey-test
  (ds/with-remote-api (:remote-api fixtures/*system*) "akvoflowsandbox"
    (testing "Filtered surveys"
      (is (= 2 (surveys-for-user "akvo.flow.user.test@gmail.com")))
      (is (= 2 (surveys-for-user "akvo.flow.user.test2@gmail.com")))
      (is (= 0 (surveys-for-user "akvo.flow.user.test3@gmail.com"))))))

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
        question #{:id :name :type :order :personal-data}))))

(deftest list-authorized-forms
  (ds/with-remote-api (:remote-api fixtures/*system*) "akvoflowsandbox"
    (testing "Filtered forms"
      (let [user-id (user/id "akvo.flow.user.test@gmail.com")
            forms (survey/list-forms-by-ids user-id #{145492013 146532016 153312013})
            forms-ids (set (map ds/id forms))]
        (is (= #{145492013 146532016} forms-ids))))))

(deftest survey-permissions
  (testing "Has permissions"
    (is (= [{:instance-id "a" :survey-id "1"}] (survey/keep-allowed-to-see
                                                 [{:instance-id "a" :survey-id "1"}]
                                                 [{:instance-id "a"
                                                   :survey-ids ["1"]}])))
    (is (= #{{:instance-id "a" :survey-id "1"} {:instance-id "b" :survey-id "2"}}
          (set (survey/keep-allowed-to-see
                 [{:instance-id "a" :survey-id "1"}
                  {:instance-id "b" :survey-id "2"}]
                 [{:instance-id "a" :survey-ids ["1"]}
                  {:instance-id "b" :survey-ids ["2"]}]))))
    (testing "compares just the instance id and the survey-id"
      (is (= [{:instance-id "a" :survey-id "1" :foo "bar"}]
            (survey/keep-allowed-to-see
              [{:instance-id "a" :survey-id "1" :foo "bar"}]
              [{:instance-id "a"
                :survey-ids ["1"]}])))))

  (testing "Not perms for the survey"
    (is (= [] (survey/keep-allowed-to-see [{:instance-id "a" :survey-id "1"}] {:instance-id "a"
                                                                               :survey-ids ["2"]}))))
  (testing "Zero permissions"
    (is (= [] (survey/keep-allowed-to-see [{:instance-id "a" :survey-id "1"}] [])))
    (is (= [] (survey/keep-allowed-to-see [{:instance-id "a" :survey-id "1"}] [nil])))
    (is (= [] (survey/keep-allowed-to-see [{:instance-id "a" :survey-id "1"}] [{:instance-id "a"
                                                                                :survey-ids []}]))))
  (testing "Same survey id, but different instance"
    (is (= [] (survey/keep-allowed-to-see [{:instance-id "a" :survey-id "1"}] [{:instance-id "b"
                                                                                :survey-ids ["1"]}])))))
