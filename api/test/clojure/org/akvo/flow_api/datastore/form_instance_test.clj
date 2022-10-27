(ns org.akvo.flow-api.datastore.form-instance-test
  (:require [clojure.test :refer [are use-fixtures deftest testing is]]
            [org.akvo.flow-api.component.remote-api]
            [org.akvo.flow-api.datastore :as ds]
            [org.akvo.flow-api.datastore.form-instance :as form-instance]
            [org.akvo.flow-api.datastore.survey :as survey]
            [org.akvo.flow-api.datastore.user :as user]
            [org.akvo.flow-api.fixtures :as fixtures])
  (:import com.google.appengine.api.datastore.DatastoreServiceFactory
           java.time.Instant
           java.util.Date))

(def system {:components
             {:remote-api #'org.akvo.flow-api.component.remote-api/local-api}
             :dependencies {:remote-api []}
             :endpoints {}
             :config {}})

(use-fixtures :once (fixtures/system system))

(deftest form-instance-pagination-test
  (ds/with-remote-api (:remote-api fixtures/*system*) "akvoflowsandbox"
    (let [ds (DatastoreServiceFactory/getDatastoreService)
          survey (survey/by-id (user/id "akvo.flow.user.test@gmail.com") "152342023")
          form (first (:forms survey))]

      (testing "Default page size"
        (is (= 30
               (count (:form-instances (form-instance/list ds form))))))

      (testing "Basic pagination"
        (is (= (:form-instances (form-instance/list ds form))
               (let [page-1 (form-instance/list ds form {:page-size 15})
                     page-2 (form-instance/list ds form {:page-size 15
                                                         :cursor (:cursor page-1)})]
                 (concat (:form-instances page-1)
                         (:form-instances page-2))))))

      (testing "End of pagination"
        (let [page-1 (form-instance/list ds form {:page-size 150})
              page-2 (form-instance/list ds form {:page-size 150
                                                  :cursor (:cursor page-1)})
              page-3 (form-instance/list ds form {:page-size 150
                                                  :cursor (:cursor page-2)})]
          (is (= 150 (count (:form-instances page-1))))
          (is (= 17 (count (:form-instances page-2))))
          (is (empty? (:form-instances page-3))))))))

(deftest form-instance-filter-test
  (ds/with-remote-api (:remote-api fixtures/*system*) "akvoflowsandbox"
    (let [ds (DatastoreServiceFactory/getDatastoreService)
          filter-date (Instant/parse "2017-03-30T08:05:00Z")
          form-id "153312013"
          data (form-instance/list ds {:id form-id} {:page-size 100
                                                     :prefetch-size 100
                                                     :submission-date filter-date
                                                     :operator ">="})
          ok? (fn [instance]
                (and (= (:form-id instance)
                        form-id)
                     (.isAfter (.toInstant ^Date (:submission-date instance)) filter-date)))]
      (is (not-empty (:form-instances data)))
      (is (every? ok? (:form-instances data))))))

(deftest response-parsing
  (testing "date question type"
    (is (= (form-instance/parse-response "DATE" "1493039527580" {})
           "2017-04-24T13:12:07.580Z"))
    (is (= (form-instance/parse-response "DATE" "29-08-2013 02:00:00 CEST" {})
           "2013-08-29T00:00:00Z")))
  (testing "photo question type"
    (is (= (form-instance/parse-response "PHOTO" "/storage/foo.png" {:asset-url-root "http://localhost"})
           {"filename" "http://localhost/foo.png"
            "location" nil}))
    (is (= (form-instance/parse-response "PHOTO" "{\"filename\": \"/storage/foo.png\", \"location\": null}"
                                         {:asset-url-root "http://localhost/"})
           {"filename" "http://localhost/foo.png"
            "location" nil})))
  (testing "video question type"
    (is (= (form-instance/parse-response "VIDEO" "/storage/foo.mpeg" {:asset-url-root "http://localhost"})
           {"filename" "http://localhost/foo.mpeg"
            "location" nil}))
    (is (= (form-instance/parse-response "VIDEO" "{\"filename\": \"/storage/foo.mpeg\", \"location\": null}"
                                         {:asset-url-root "http://localhost/"})
           {"filename" "http://localhost/foo.mpeg"
            "location" nil})))
  (testing "geo question type"
    (is (= (form-instance/parse-response "GEO" "1.0|2.0|3.0|foo" {})
           {:lat 1.0
            :long 2.0
            :elev 3.0
            :code "foo"}))
    (is (= (form-instance/parse-response "GEO" "|||" {})
           nil))
    (is (= (form-instance/parse-response "GEO" "1.0|2.0||" {})
           {:lat 1.0
            :long 2.0
            :elev nil
            :code nil})))
  (testing "caddisfly question type"
    (let [data "{\"type\":\"caddisfly\",\"name\":\"Water - E.coli\",\"uuid\":\"e40d4764-e73f-46dd-a598-ed4db0fd3386\",\"result\":[],\"image\":\"a3bf6305-add0-49df-9cd7-75dab425e45e.jpg\",\"testDate\":\"2019-10-29 17:17\",\"app\":{\"appVersion\":\"1.0.13 (Build 275)\"}}"]
      (is (= (form-instance/parse-response "CADDISFLY" data {:asset-url-root "https://example.org/images"})
             {"type" "caddisfly", "name" "Water - E.coli", "uuid" "e40d4764-e73f-46dd-a598-ed4db0fd3386", "result" [], "image" "https://example.org/images/a3bf6305-add0-49df-9cd7-75dab425e45e.jpg", "testDate" "2019-10-29 17:17", "app" {"appVersion" "1.0.13 (Build 275)"}})))))

(deftest response-for-missing-question-test
  (testing "Response for missing question (regression #82)"
    (with-redefs [form-instance/response-entity->map
                  (constantly {:form-instance-id "1"
                               :question-id "2"
                               :response-str "42"
                               :iteration 0})]
      (let [update-form-instances (form-instance/update-form-instances-fn {} {} {})]
        (is (= (update-form-instances {} 'entity)
               {})))
      (let [update-form-instances (form-instance/update-form-instances-fn
                                   {"2" "NUMBER"} ;; Question id -> Question type
                                   {"2" "4"} ;; Question id -> Question group id
                                   {} ;; Opts
                                   )]
        (is (= (form-instance/vectorize-response-iterations (update-form-instances {} 'entity))
               {"1" {"4" [{"2" 42.0}]}}))))))

(deftest option-response-format
  (testing "Responses for option questions (regression #86)"
    (are [str-response api-response] (= (form-instance/parse-response "OPTION"
                                                                      str-response
                                                                      {})
                                        api-response)
      "6" [{"text" "6"}]
      "A|B" [{"text" "A"} {"text" "B"}]
      "[{\"text\": \"A\"}, {\"text\": \"B\"}]" [{"text" "A"} {"text" "B"}]
      "[{\"text\": \"A\", \"code\": \"a\"},
        {\"text\": \"B\", \"code\": \"b\", \"isOther\": true}]"
      [{"text" "A" "code" "a"}
       {"text" "B" "code" "b" "isOther" true}])))
