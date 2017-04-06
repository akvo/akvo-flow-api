(ns org.akvo.flow-api.datastore.user-test
  (:require  [clojure.test :refer :all]
             [org.akvo.flow-api.datastore :as ds]
             [org.akvo.flow-api.datastore.user :as user]
             [org.akvo.flow-api.fixtures :as fixtures]))

(def system {:components
             {:remote-api #'org.akvo.flow-api.component.remote-api/local-api}
             :dependencies {:remote-api []}
             :endpoints {}
             :config {}})

(use-fixtures :once (fixtures/system system))

(deftest user-tests
  (ds/with-remote-api (:remote-api fixtures/*system*) "akvoflowsanbox"
    (testing "Non existing user"
      (is (thrown? clojure.lang.ExceptionInfo
                   (user/id "no-such@user.com"))))

    (testing "Existing users"
      (are [email] (number? (user/id email))
        "akvo.flow.user.test@gmail.com"
        "akvo.flow.user.test2@gmail.com"
        "akvo.flow.user.test3@gmail.com"))))
