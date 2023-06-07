(ns org.akvo.flow-api.datastore.device-test
  (:require [clojure.test :refer [deftest testing is use-fixtures]]
            [org.akvo.flow-api.component.remote-api]
            [org.akvo.flow-api.datastore :as ds]
            [org.akvo.flow-api.datastore.device :as device]
            [org.akvo.flow-api.fixtures :as fixtures]))

(def system {:components
             {:remote-api #'org.akvo.flow-api.component.remote-api/local-api}
             :dependencies {:remote-api []}
             :endpoints {}
             :config {}})

(use-fixtures :once (fixtures/system system))

(deftest device-test
  (ds/with-remote-api (:remote-api fixtures/*system*) "akvoflowsandbox"
    (let [all-devices (device/list)]

      (testing "Device list"
        (is (= 2 (count all-devices)))))))
