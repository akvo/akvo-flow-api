(ns org.akvo.flow-api.end-to-end
  (:require [clojure.test :refer :all]
            [clj-http.client]
            [org.akvo.flow-api.fixtures :as fixtures]))

(use-fixtures :once (fn [f]
                      (fixtures/check-servers-up)
                      (f)))

(deftest huge-headers
  (testing "Keycloak Bearer token header is bigger than 8k"
    (is (= 200
          (:status (clj-http.client/get "http://mainnetwork:3000/"
                     {:as :json
                      :headers {"huge" (apply str (repeat 30000 "x"))}
                      :content-type :json}))))))

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