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
