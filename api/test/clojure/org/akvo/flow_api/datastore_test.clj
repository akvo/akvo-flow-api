(ns org.akvo.flow-api.datastore-test
  (:require  [akvo.commons.gae.query :as q]
             [clojure.test :refer [use-fixtures deftest testing is]]
             [org.akvo.flow-api.component.remote-api]
             [org.akvo.flow-api.datastore :as ds]
             [org.akvo.flow-api.fixtures :as fixtures])
  (:import [com.google.appengine.api.datastore DatastoreServiceFactory]
           java.util.Date))

(def system {:components
             {:remote-api #'org.akvo.flow-api.component.remote-api/local-api}
             :endpoints {}
             :config {}})

(use-fixtures :once (fixtures/system system))

(deftest reducible-queries
  (let [remote-api (:remote-api fixtures/*system*)]
    (ds/with-remote-api remote-api "akvoflowsandbox"
      (let [dss (DatastoreServiceFactory/getDatastoreService)
            now (Date.)]
        (testing "Getting more than 1K entities"
          (is (> (count
                  (into []
                        identity
                        (ds/reducible-gae-query dss
                                                {:kind "QuestionAnswerStore"
                                                 :filter (q/< "createdDateTime" now)
                                                 :projections {"value" String}}
                                                {})))
                 1000)))))))
