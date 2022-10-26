(ns org.akvo.flow-api.datastore.user-test
  (:require  [akvo.commons.gae :as gae]
             [akvo.commons.gae.query :as query]
             [clojure.test :refer [use-fixtures deftest testing is are]]
             [org.akvo.flow-api.boundary.user :as user-cache]
             [org.akvo.flow-api.component.cache]
             [org.akvo.flow-api.component.remote-api]
             [org.akvo.flow-api.datastore :as ds]
             [org.akvo.flow-api.datastore.user :as user]
             [org.akvo.flow-api.fixtures :as fixtures])
  (:import [com.google.appengine.api.datastore DatastoreServiceFactory]))

(defn uuid [] (str (java.util.UUID/randomUUID)))
(def local-ds {:hostname "localhost" :port 8888})

(def system {:components
             {:remote-api #'org.akvo.flow-api.component.remote-api/local-api
              :user-cache #'org.akvo.flow-api.component.cache/ttl-memory-cache
              :unknown-user-cache #'org.akvo.flow-api.component.cache/ttl-memory-cache}
             :dependencies {:remote-api []
                            :user-cache []}
             :endpoints {}
             :config {:user-cache {:ttl 86400000}
                      :unknown-user-cache {:ttl 600000}}})

(use-fixtures :once (fixtures/system system))

(deftest user-tests
  (ds/with-remote-api (:remote-api fixtures/*system*) "akvoflowsanbox"
    (testing "Non existing user"
      (is (nil? (user/id "no-such@user.com"))))

    (testing "Existing users"
      (are [email] (number? (user/id email))
        "akvo.flow.user.test@gmail.com"
        "akvo.flow.user.test2@gmail.com"
        "akvo.flow.user.test3@gmail.com"))))

(defn find-user [ds unique-email]
  (first
   (iterator-seq
    (.iterator (query/result ds
                             {:kind "User"
                              :filter (query/= "emailAddress" unique-email)})))))

(defn create-user [ds unique-email]
  (gae/put! ds "User" {"emailAddress" unique-email})

  (fixtures/try-for "GAE took too long to return results" 10
    (find-user ds unique-email)))

(defn delete-user [ds unique-email]
  (let [user (find-user ds unique-email)]
    (.delete ds (into-array [(.getKey user)])))

  (fixtures/try-for "GAE took too long to return results" 10
    (not (find-user ds unique-email))))

(deftest user-cache
  (let [remote-api (assoc (:remote-api fixtures/*system*)
                          :user-cache (:user-cache fixtures/*system*)
                          :unknown-user-cache (:unknown-user-cache fixtures/*system*))]
    (ds/with-remote-api remote-api "akvoflowsandbox"
      (let [ds (DatastoreServiceFactory/getDatastoreService)]
        (testing "User is cached"
          (let [unique-email (uuid)]
            (create-user ds unique-email)
            (is (some? (user-cache/id-by-email remote-api "akvoflowsanbox" unique-email)))
            (delete-user ds unique-email)
            (is (some? (user-cache/id-by-email remote-api "akvoflowsanbox" unique-email)))))

        (testing "Cache also if the user does not exist"
          (let [unique-email (uuid)]
            (is (nil? (user-cache/id-by-email remote-api "akvoflowsanbox" unique-email)))
            (create-user ds unique-email)
            (is (nil? (user-cache/id-by-email remote-api "akvoflowsanbox" unique-email)))))))))
