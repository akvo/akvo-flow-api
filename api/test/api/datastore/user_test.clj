(ns api.datastore.survey-test
  (:require  [api.datastore :as ds]
             [api.datastore.user :as user]
             [clojure.test :refer :all]))

(deftest user-tests
  (ds/with-local-api
    (testing "Non existing user"
      (is (thrown? clojure.lang.ExceptionInfo
                   (user/id "no-such@user.com"))))

    (testing "Existing users"
      (are [email] (number? (user/id email))
        "akvo.flow.user.test@gmail.com"
        "akvo.flow.user.test2@gmail.com"
        "akvo.flow.user.test3@gmail.com"))))
