(ns api.datastore.survey-test
  (:require [api.datastore :as ds]
            [api.datastore.survey :as survey]
            [api.datastore.user :as user]
            [clojure.test :refer :all])
  (:import [com.google.appengine.api.datastore DatastoreServiceFactory]))

(deftest survey-test
  (ds/with-local-api
    (let [ds (DatastoreServiceFactory/getDatastoreService)
          user-1 (user/id "akvo.flow.user.test@gmail.com")
          user-2 (user/id "akvo.flow.user.test2@gmail.com")
          user-3 (user/id "akvo.flow.user.test3@gmail.com")
          folder-id "153142013"
          surveys-1 (survey/list user-1 folder-id)
          surveys-2 (survey/list user-2 folder-id)
          surveys-3 (survey/list user-3 folder-id)]
      (testing "Filtered surveys"
        (is (= 2 (count surveys-1)))
        (is (= 2 (count surveys-2)))
        (is (= 0 (count surveys-3)))))))
