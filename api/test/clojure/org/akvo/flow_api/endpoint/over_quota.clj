(ns org.akvo.flow-api.endpoint.over-quota
  (:require [clojure.test :refer [use-fixtures deftest is]]
            [org.akvo.flow-api.component.akvo-flow-server-config]
            [org.akvo.flow-api.component.remote-api]
            [org.akvo.flow-api.endpoint.survey :as survey]
            [org.akvo.flow-api.fixtures :as fixtures]
            [org.akvo.flow-api.middleware.anomaly :as anomaly]
            [ring.mock.request :as mock]))

(def system {:components
             {:remote-api #'org.akvo.flow-api.component.remote-api/over-quota-api
              :akvo-flow-server-config #'org.akvo.flow-api.component.akvo-flow-server-config/dummy-akvo-flow-server-config}
             :dependencies {:remote-api [:akvo-flow-server-config]}
             :config {}})

(use-fixtures :once (fixtures/system system))

(deftest gae-instance-over-quota
  (let [handler (-> fixtures/*system*
                    (survey/endpoint)
                    (anomaly/wrap-log-errors)
                    (anomaly/wrap-anomaly))
        req (->
             (mock/request :get "/orgs/akvoflowsandbox/surveys/148412306")
             (assoc :email "akvo.flow.user.test@gmail.com"))
        resp (handler req)]
    (is (= 429 (:status resp)))))
