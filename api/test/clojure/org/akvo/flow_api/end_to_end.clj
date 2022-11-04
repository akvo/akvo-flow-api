(ns org.akvo.flow-api.end-to-end
  (:require [clojure.test :refer [use-fixtures deftest testing is]]
            [clj-http.client]
            [org.akvo.flow-api.fixtures :as fixtures])
  (:import clojure.lang.ExceptionInfo))

(use-fixtures :once (fn [f]
                      (fixtures/check-servers-up)
                      (f)))

(deftest huge-headers
  (testing "Keycloak Bearer token header is bigger than 8k"
    (is (= 200
          (:status (clj-http.client/get "http://localhost:3000/"
                     {:as :json
                      :headers {"huge" (apply str (repeat 30000 "x"))}
                      :content-type :json}))))))

(deftest answer-stats
  (testing "Random email trying to get a statistic"
    (is (thrown-with-msg? ExceptionInfo #"status 403"
        (try
          (clj-http.client/get "http://localhost:3000/orgs/akvoflowsandbox/stats"
                               {:as :json
                                :headers {"x-akvo-email" "random.email@gmail.com"}
                                :query-params {:survey_id "148412306"
                                               :form_id "145492013"
                                               :question_id "147432013"}
                                :content-type :json})
          (catch ExceptionInfo e
            (is (= (-> e ex-data :status) 403))
            (is (.contains ^String (-> e ex-data :body) "User does not exist"))
            (throw e))))))
  (testing "Survey Not Found"
    (is (thrown-with-msg? ExceptionInfo #"status 404"
                          (try
                            (clj-http.client/get "http://localhost:3000/orgs/akvoflowsandbox/stats"
                                                 {:as :json
                                                  :headers {"x-akvo-email" "akvo.flow.user.test@gmail.com"}
                                                  :query-params {:survey_id "999"
                                                                 :form_id "145492013"
                                                                 :question_id "147432013"}
                                                  :content-type :json})
                            (catch ExceptionInfo e
                              (is (= (-> e ex-data :status) 404))
                              (is (.contains ^String (-> e ex-data :body) "Survey not found"))
                              (throw e))))))
  (testing "Question Not Found"
    (is (thrown-with-msg? ExceptionInfo #"status 404"
                          (try
                            (clj-http.client/get "http://localhost:3000/orgs/akvoflowsandbox/stats"
                                                 {:as :json
                                                  :headers {"x-akvo-email" "akvo.flow.user.test@gmail.com"}
                                                  :query-params {:survey_id "148412306"
                                                                 :form_id "145492013"
                                                                 :question_id "999"}
                                                  :content-type :json})
                            (catch ExceptionInfo e
                              (is (= (-> e ex-data :status) 404))
                              (is (.contains ^String (-> e ex-data :body) "Question not found"))
                              (throw e))))))
  (testing "Question is not an Option Question"
    (is (thrown-with-msg? ExceptionInfo #"status 400"
                          (try
                            (clj-http.client/get "http://localhost:3000/orgs/akvoflowsandbox/stats"
                                                 {:as :json
                                                  :headers {"x-akvo-email" "akvo.flow.user.test@gmail.com"}
                                                  :query-params {:survey_id "148412306"
                                                                 :form_id "145492013"
                                                                 :question_id "148442013"}
                                                  :content-type :json})
                            (catch ExceptionInfo e
                              (is (= (-> e ex-data :status) 400))
                              (prn (-> e ex-data :body))
                              (is (.contains ^String (-> e ex-data :body) "Not an OPTION question"))
                              (throw e))))))
  (testing "Answer summary of an option type of question"
    (let [response (clj-http.client/get "http://mainnetwork:3000/orgs/akvoflowsandbox/stats"
                     {:as :json
                      :headers {"x-akvo-email" "akvo.flow.user.test@gmail.com"}
                      :query-params {:survey_id "148412306"
                                     :form_id "145492013"
                                     :question_id "147432013"}
                      :content-type :json})]
      (is (= 200 (:status response)))
      (is (= 2 (-> response :body :Kpalbe))))))

(deftest form-instances
  (testing "Submission date filter is optional"
    (let [response (clj-http.client/get "http://mainnetwork:3000/orgs/akvoflowsandbox/form_instances"
                     {:as :json
                      :headers {"x-akvo-email" "akvo.flow.user.test@gmail.com"}
                      :query-params {:survey_id "152342023"
                                     :form_id "146532016"
                                     :page_size 2}
                      :content-type :json})]
      (is (= 200 (:status response)))
      (is (= 2 (-> response :body :formInstances count))))))

(deftest data-point
  (testing "Datapoint"
    (let [response (clj-http.client/get "http://mainnetwork:3000/orgs/akvoflowsandbox/data_points"
                     {:as :json
                      :headers {"x-akvo-email" "akvo.flow.user.test@gmail.com"}
                      :query-params {:survey_id "152342023"
                                     :form_id "146532016"
                                     :page_size 2}
                      :content-type :json})]
      (is (= 200 (:status response)))
      (is (= 2 (-> response :body :dataPoints count))))))

(deftest surveys
  (testing "All surveys"
    (let [response (clj-http.client/get "http://mainnetwork:3000/orgs/akvoflowsandbox/surveys"
                     {:as :json
                      :headers {"x-akvo-email" "akvo.flow.user.test@gmail.com"}
                      :query-params {:folder_id "153142013"}
                      :content-type :json})]
      (is (= 200 (:status response)))
      (is (= 2 (-> response :body :surveys count)))))

  (testing "One survey"
    (let [response (clj-http.client/get "http://mainnetwork:3000/orgs/akvoflowsandbox/surveys/152342023"
                     {:as :json
                      :headers {"x-akvo-email" "akvo.flow.user.test@gmail.com"}
                      :content-type :json})]
      (is (= 200 (:status response)))
      (is (= 1 (-> response :body :forms count))))))

(deftest folder
  (testing "Root folder"
    (let [response (clj-http.client/get "http://mainnetwork:3000/orgs/akvoflowsandbox/folders"
                     {:as :json
                      :headers {"x-akvo-email" "akvo.flow.user.test@gmail.com"}
                      :content-type :json})]
      (is (= 200 (:status response)))
      (is (= 1 (-> response :body :folders count))))))
