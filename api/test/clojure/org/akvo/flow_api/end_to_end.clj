(ns org.akvo.flow-api.end-to-end
  (:require [cheshire.core :as json]
            [clj-http.client]
            [clojure.test :refer [use-fixtures deftest testing is]]
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

(defn http-result [^ExceptionInfo e]
  (-> e
      ex-data
      (select-keys [:status :body])
      (update :body #(json/parse-string % true))))

(defn test-error [expected-status expected-message http-url http-opts]
  (is (thrown-with-msg?
       ExceptionInfo (re-pattern (str "status " expected-status))
       (try
         (clj-http.client/get http-url
                              (merge {:as :json
                                      :content-type :json}
                                     http-opts))
         (catch ExceptionInfo e
           (let [{{message :message} :body
                  :keys [status]} (http-result e)]
             (is (= expected-status status))
             (is (= expected-message message)))
           (throw e))))))

(deftest answer-statistic
  (testing "Random email trying to get a statistic"
    (test-error 403
                "User does not exist"
                "http://localhost:3000/orgs/akvoflowsandbox/stats"
                {:headers {"x-akvo-email" "random.email@gmail.com"}
                 :query-params {:survey_id "148412306"
                                :form_id "145492013"
                                :question_id "147432013"}}))
  (testing "Survey Not Found"
    (test-error 404
                "Survey not found"
                "http://localhost:3000/orgs/akvoflowsandbox/stats"
                {:headers {"x-akvo-email" "akvo.flow.user.test@gmail.com"}
                 :query-params {:survey_id "999"
                                :form_id "145492013"
                                :question_id "147432013"}}))
  (testing "Question Not Found"
    (test-error 404
                "Question not found"
                "http://localhost:3000/orgs/akvoflowsandbox/stats"
                {:headers {"x-akvo-email" "akvo.flow.user.test@gmail.com"}
                 :query-params {:survey_id "148412306"
                                :form_id "145492013"
                                :question_id "999"}}))
  (testing "Question is not an Option Question"
    (test-error 400
                "Not an [Option|Number] question"
                "http://localhost:3000/orgs/akvoflowsandbox/stats"
                {:headers {"x-akvo-email" "akvo.flow.user.test@gmail.com"}
                 :query-params {:survey_id "148412306"
                                :form_id "145492013"
                                :question_id "148442013"}}))
  (testing "Answer summary of an option type of question"
    (let [response (clj-http.client/get "http://mainnetwork:3000/orgs/akvoflowsandbox/stats"
                     {:as :json
                      :headers {"x-akvo-email" "akvo.flow.user.test@gmail.com"}
                      :query-params {:survey_id "148412306"
                                     :form_id "145492013"
                                     :question_id "147432013"}
                      :content-type :json})]
      (is (= 200 (:status response)))
      (is (= 2 (-> response :body :Kpalbe))))
    (let [response (clj-http.client/get "http://mainnetwork:3000/orgs/akvoflowsandbox/stats"
                     {:as :json
                      :headers {"x-akvo-email" "akvo.flow.user.test@gmail.com"}
                      :query-params {:survey_id "148412306"
                                     :form_id "145492013"
                                     :question_id "146622024"}
                      :content-type :json})]
      (is (= 200 (:status response)))
      (is (= 365.0 (-> response :body :max)))
      (is (= "max-age=120" (get-in response [:headers "Cache-Control"]))))))

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
