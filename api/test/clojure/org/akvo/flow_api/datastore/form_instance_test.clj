(ns org.akvo.flow-api.datastore.form-instance-test
  (:require [clojure.test :refer :all]
            [org.akvo.flow-api.datastore :as ds]
            [org.akvo.flow-api.datastore.form-instance :as form-instance]
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

(deftest form-instance-pagination-test
  (ds/with-remote-api (:remote-api fixtures/*system*) "akvoflowsandbox"
    (let [ds (DatastoreServiceFactory/getDatastoreService)
          survey (survey/by-id (user/id "akvo.flow.user.test@gmail.com") "152342023")
          form (first (:forms survey))]

      (testing "Default page size"
        (is (= 30
               (count (:form-instances (form-instance/list ds form))))))

      (testing "Basic pagination"
        (is (= (:form-instances (form-instance/list ds form))
               (let [page-1 (form-instance/list ds form {:page-size 15})
                     page-2 (form-instance/list ds form {:page-size 15
                                                         :cursor (:cursor page-1)})]
                 (concat (:form-instances page-1)
                         (:form-instances page-2))))))

      (testing "End of pagination"
        (let [page-1 (form-instance/list ds form {:page-size 150})
              page-2 (form-instance/list ds form {:page-size 150
                                                  :cursor (:cursor page-1)})
              page-3 (form-instance/list ds form {:page-size 150
                                                  :cursor (:cursor page-2)})]
          (is (= 150 (count (:form-instances page-1))))
          (is (= 17 (count (:form-instances page-2))))
          (is (empty? (:form-instances page-3))))))))

(deftest response-parsing
  (testing "date question type"
    (is (= (form-instance/parse-response "DATE" "1493039527580")
           "2017-04-24T13:12:07.580Z"))
    (is (= (form-instance/parse-response "DATE" "29-08-2013 02:00:00 CEST")
           "2013-08-29T00:00:00Z"))))
