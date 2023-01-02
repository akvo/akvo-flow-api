(ns org.akvo.flow-api.datastore.folder-test
  (:require [clojure.test :refer [deftest testing is use-fixtures]]
            [org.akvo.flow-api.component.remote-api]
            [org.akvo.flow-api.datastore :as ds]
            [org.akvo.flow-api.datastore.folder :as folder]
            [org.akvo.flow-api.datastore.user :as user]
            [org.akvo.flow-api.fixtures :as fixtures]))

(def system {:components
             {:remote-api #'org.akvo.flow-api.component.remote-api/local-api}
             :dependencies {:remote-api []}
             :endpoints {}
             :config {}})

(use-fixtures :once (fixtures/system system))

(deftest folder-test
  (ds/with-remote-api (:remote-api fixtures/*system*) "akvoflowsandbox"
    (let [user-1 (user/id "akvo.flow.user.test@gmail.com")
          user-2 (user/id "akvo.flow.user.test2@gmail.com")
          user-3 (user/id "akvo.flow.user.test3@gmail.com")
          root-folders-1 (folder/list user-1 "0")
          root-folders-2 (folder/list user-2 "0")
          root-folders-3 (folder/list user-3 "0")
          folder-with-nested-folders (:id (first (sort-by :id root-folders-2)))]

      (testing "Root folder"
        (is (= 1 (count root-folders-1)))
        (is (= 2 (count root-folders-2)))
        (is (= 1 (count root-folders-3)))
        (is (not= root-folders-1 root-folders-3)))

      (testing "Nested folders"
        (let [nested-folders-1 (folder/list user-1 folder-with-nested-folders)
              nested-folders-2 (folder/list user-2 folder-with-nested-folders)
              nested-folders-3 (folder/list user-3 folder-with-nested-folders)]
          (is (= 0 (count nested-folders-1)))
          (is (= 2 (count nested-folders-2)))
          (is (= 2 (count nested-folders-3))))))))
