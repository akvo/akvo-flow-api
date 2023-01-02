(ns org.akvo.flow-api.datastore.data-point-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            org.akvo.flow-api.component.remote-api
            [org.akvo.flow-api.datastore :as ds]
            [org.akvo.flow-api.datastore.data-point :as data-point]
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

(deftest data-point-pagination-test
  (ds/with-remote-api (:remote-api fixtures/*system*) "akvoflowsandbox"
    (let [ds (DatastoreServiceFactory/getDatastoreService)
          survey (survey/by-id (user/id "akvo.flow.user.test@gmail.com") "152342023")]

      (testing "Default page size"
        (is (= 30
               (count (:data-points (data-point/list ds survey))))))

      (testing "Basic pagination"
        (is (= (:data-points (data-point/list ds survey {}))
               (let [page-1 (data-point/list ds survey {:page-size 15})
                     page-2 (data-point/list ds survey {:page-size 15
                                                        :cursor (:cursor page-1)})]
                 (concat (:data-points page-1)
                         (:data-points page-2))))))

      (testing "End of pagination"
        (let [page-1 (data-point/list ds survey {:page-size 150})
              page-2 (data-point/list ds survey {:page-size 150
                                                 :cursor (:cursor page-1)})
              page-3 (data-point/list ds survey {:page-size 150
                                                 :cursor (:cursor page-2)})]
          (is (= 150 (count (:data-points page-1))))
          (is (= 17 (count (:data-points page-2))))
          (is (empty? (:data-points page-3))))))))
