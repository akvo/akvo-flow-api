(ns api.datastore.form-instance-test
  (:require [api.datastore :as ds]
            [api.datastore.form-instance :as form-instance]
            [api.datastore.survey :as survey]
            [clojure.test :refer :all])
  (:import [com.google.appengine.api.datastore DatastoreServiceFactory]))

(deftest form-instance-pagination-test
  (ds/with-local-api
    (let [ds (DatastoreServiceFactory/getDatastoreService)
          survey (survey/get-survey-definition "akvo.flow.user.test@gmail.com" "152342023")
          form (first (:forms survey))]

      (testing "Default page size"
        (is (= 30
               (count (:form-instances (form-instance/fetch-form-instances ds form))))))

      (testing "Basic pagination"
        (is (= (:form-instances (form-instance/fetch-form-instances ds form))
               (let [page-1 (form-instance/fetch-form-instances ds form {:page-size 15})
                     page-2 (form-instance/fetch-form-instances ds form {:page-size 15
                                                                         :cursor (:cursor page-1)})]
                 (concat (:form-instances page-1)
                         (:form-instances page-2))))))

      (testing "End of pagination"
        (let [page-1 (form-instance/fetch-form-instances ds form {:page-size 150})
              page-2 (form-instance/fetch-form-instances ds form {:page-size 150
                                                                  :cursor (:cursor page-1)})
              page-3 (form-instance/fetch-form-instances ds form {:page-size 150
                                                                  :cursor (:cursor page-2)})]
          (is (= 150 (count (:form-instances page-1))))
          (is (= 17 (count (:form-instances page-2))))
          (is (empty? (:form-instances page-3))))))))
