(ns org.akvo.flow-api.datastore.form-instance
  (:require [akvo.commons.gae :as gae]
            [akvo.commons.gae.query :as q]
            [cheshire.core :as json]
            [clojure.string :as s]
            [org.akvo.flow-api.anomaly :as anomaly]
            [org.akvo.flow-api.datastore :as ds])
  (:refer-clojure :exclude [list])
  (:import [com.fasterxml.jackson.core JsonParseException]
           [com.google.appengine.api.datastore Entity Text QueryResultIterator QueryResultIterable]))

(set! *warn-on-reflection* true)

(def MAX_PAGE_SIZE 300)

(defn normalize-page-size [page-size]
  (if-not page-size
    30 ;; default
    (if (<= page-size MAX_PAGE_SIZE)
      page-size
      MAX_PAGE_SIZE)))

(defn form-instances-query
  ^com.google.appengine.api.datastore.QueryResultIterator
  [ds form-definition {:keys [cursor page-size]}]
  (let [page-size (normalize-page-size page-size)]
    (.iterator ^QueryResultIterable (q/result ds
                                              {:kind "SurveyInstance"
                                               :filter (q/= "surveyId" (Long/parseLong (:id form-definition)))}
                                              {:start-cursor cursor
                                               :chunk-size page-size
                                               :limit page-size}))))

(defn question-type-map
  "Builds a map from question id to type"
  [form-definition]
  (reduce (fn [question-types {:keys [id type]}]
            (assoc question-types id type))
          {}
          (mapcat :questions (:question-groups form-definition))))

;; FREE_TEXT, OPTION, NUMBER, GEO, PHOTO, VIDEO, SCAN, TRACK,
;; NAME, STRENGTH, DATE, CASCADE, GEOSHAPE, SIGNATURE
(defmulti parse-response (fn [type response-str] type))

(defmethod parse-response "FREE_TEXT"
  [_ response-str]
  response-str)

(defmethod parse-response "OPTION"
  [_ response-str]
  (try (json/parse-string response-str)
       (catch JsonParseException _)))

(defn parse-double [s]
  (try (Double/parseDouble s)
       (catch NumberFormatException _)))

(defmethod parse-response "NUMBER"
  [_ response-str]
  (parse-double response-str))

;; 52.40376391|-1.75630525|189.6|6oqmgtjv
(defmethod parse-response "GEO"
  [_ response-str]
  (let [[lat long elev code] (s/split response-str #"\|")]
    {:lat (parse-double lat)
     :long (parse-double long)
     :elev (parse-double elev)
     :code code}))

;; {"filename":"/storage/.../.jpg","location":null}
;; or
;; /storage/.../.jpg
;; TODO: If location is non-null, what is the format?
(defmethod parse-response "PHOTO"
  [_ response-str]
  (try (json/parse-string response-str)
       (catch JsonParseException _
         {"filename" response-str
          "location" nil})))

(defmethod parse-response "VIDEO"
  [_ response-str]
  (try (json/parse-string response-str)
       (catch JsonParseException _
         {"filename" response-str
          "location" nil})))

(defmethod parse-response "SCAN"
  [_ response-str]
  response-str)

(defmethod parse-response "TRACK"
  [_ response-str]
  response-str)

(defmethod parse-response "NAME"
  [_ response-str]
  response-str)

(defmethod parse-response "STRENGTH"
  [_ response-str]
  response-str)

(defmethod parse-response "DATE"
  [_ response-str]
  (ds/to-iso-8601 (java.util.Date. (Long/parseLong response-str))))

(defmethod parse-response "CASCADE"
  [_ response-str]
  (try (json/parse-string response-str)
       (catch JsonParseException _)))

(defmethod parse-response "GEOSHAPE"
  [_ response-str]
  (try (json/parse-string response-str)
       (catch JsonParseException _)))

(defmethod parse-response "SIGNATURE"
  [_ response-str]
  (try (json/parse-string response-str)
       (catch JsonParseException _)))

(defn fetch-answers [ds form-definition form-instances]
  (let [question-types (question-type-map form-definition)]
    (reduce (fn [form-instance-answers form-instance-batch]
              (reduce (fn [fia ^Entity answer]
                        (let [form-instance-id (str (.getProperty answer "surveyInstanceId"))
                              question-id (.getProperty answer "questionID")
                              response-str (or (.getProperty answer "value")
                                               (.getValue ^Text (.getProperty answer "valueText")))
                              iteration (or (.getProperty answer "iteration") 0)
                              response (when response-str
                                         (parse-response (get question-types question-id)
                                                         response-str))]
                          (assoc-in fia
                                    [form-instance-id
                                     question-id
                                     iteration]
                                    response)))
                      form-instance-answers
                      (q/result ds
                                {:kind "QuestionAnswerStore"
                                 :filter (q/in "surveyInstanceId"
                                               (map (comp #(Long/parseLong %) :id)
                                                    form-instance-batch))}
                                {:chunk-size 300})))
            {}
            (partition-all 30 form-instances))))

(defn form-instance-entity->map [^Entity form-instance]
  {:id (-> form-instance .getKey .getId str)
   :form-id (str (.getProperty form-instance "surveyId"))
   :surveyal-time (.getProperty form-instance "surveyalTime")
   :submitter (.getProperty form-instance "submitterName")
   :submission-date (.getProperty form-instance "collectionDate")
   :device-identifier (.getProperty form-instance "deviceIdentifier")
   :data-point-id (str (.getProperty form-instance "surveyedLocaleId"))
   :identifier (.getProperty form-instance "surveyedLocaleIdentifier")
   :display-name (.getProperty form-instance "surveyedLocaleDisplayName")})

(defn list
  ([ds form-definition]
   (list ds form-definition {}))
  ([ds form-definition opts]
   (let [form-instances-iterator (form-instances-query ds form-definition opts)
         form-instances (mapv form-instance-entity->map (iterator-seq form-instances-iterator))
         cursor (let [cursor (.toWebSafeString (.getCursor form-instances-iterator))]
                  (when-not (empty? cursor)
                    cursor))
         answers (fetch-answers ds form-definition form-instances)]
     {:form-instances (mapv (fn [form-instance]
                              (assoc form-instance
                                     :responses
                                     (get answers (:id form-instance))))
                            form-instances)
      :cursor cursor})))
