(ns org.akvo.flow-api.datastore.form-instance-test
  (:require [org.akvo.flow-api.datastore :as ds]
            [org.akvo.flow-api.datastore.form-instance :as form-instance]
            [org.akvo.flow-api.datastore.survey :as survey]
            [org.akvo.flow-api.datastore.user :as user]
            [clojure.test :refer :all])
  (:import [com.google.appengine.api.datastore DatastoreServiceFactory]))

(deftest form-instance-pagination-test
  (ds/with-local-api
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
