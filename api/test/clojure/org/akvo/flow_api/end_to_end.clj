(ns org.akvo.flow-api.end-to-end
  (:require [clojure.test :refer :all]
            [clj-http.client]
            [org.akvo.flow-api.fixtures :as fixtures]))

(use-fixtures :once (fn [f]
                      (fixtures/check-servers-up)
                      (f)))

(defn check-perms [user surveys]
  (->
    (clj-http.client/post "http://mainnetwork:3000/check_permissions"
      {:as :json
       :headers {"x-akvo-email" user}
       :form-params surveys
       :content-type :json})
    :body
    set
    ))

(deftest check-permissions-endpoint
  (testing "valid user"
    (let [valid-surveys [{:instance_id "akvoflowsandbox"
                          :survey_id "152342023"
                          :additional-properties "some-additional-property"}
                         {:instance_id "akvoflowsandbox"
                          :survey_id "148412306"}]
          invalid-survey {:instance_id "akvoflowsandbox"
                          :survey_id "14841230600000"}
          invalid-instance {:instance_id "unknown instance"
                            :survey_id "1"}]
      (is (=
            (set valid-surveys)
            (check-perms "akvo.flow.user.test@gmail.com"
              (conj valid-surveys
                invalid-survey
                invalid-instance))))))

  (testing "invalid user"
    (let [valid-surveys [{:instance_id "akvoflowsandbox"
                          :survey_id "152342023"}]]
      (is (=
            #{}
            (check-perms "unknown@user.com"
              valid-surveys))))))

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
