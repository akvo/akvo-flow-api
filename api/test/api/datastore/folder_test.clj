(ns api.datastore.folder-test
  (:require [api.datastore :as ds]
            [api.datastore.folder :as folder]
            [clojure.test :refer :all])
  (:import [com.google.appengine.api.datastore DatastoreServiceFactory]))

(deftest folder-test
  (ds/with-local-api
    (let [ds (DatastoreServiceFactory/getDatastoreService)
          user-1 "akvo.flow.user.test@gmail.com"
          user-2 "akvo.flow.user.test2@gmail.com"
          user-3 "akvo.flow.user.test3@gmail.com"
          root-folders-1 (folder/get-filtered-folders user-1 "0")
          root-folders-2 (folder/get-filtered-folders user-2 "0")
          root-folders-3 (folder/get-filtered-folders user-3 "0")
          folder-with-nested-folders (:id (first (sort-by :id root-folders-2)))]

      (testing "Root folder"
        (is (= 1 (count root-folders-1)))
        (is (= 2 (count root-folders-2)))
        (is (= 1 (count root-folders-3)))
        (is (not= root-folders-1 root-folders-3)))

      (testing "Nested folders"
        (let [nested-folders-1 (folder/get-filtered-folders user-1 folder-with-nested-folders)
              nested-folders-2 (folder/get-filtered-folders user-2 folder-with-nested-folders)
              nested-folders-3 (folder/get-filtered-folders user-3 folder-with-nested-folders)]
          (is (= 0 (count nested-folders-1)))
          (is (= 2 (count nested-folders-2)))
          (is (= 2 (count nested-folders-3))))))))
