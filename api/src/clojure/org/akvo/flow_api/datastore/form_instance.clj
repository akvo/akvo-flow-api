(ns org.akvo.flow-api.datastore.form-instance
  (:require [akvo.commons.gae :as gae]
            [akvo.commons.gae.query :as q]
            [cheshire.core :as json]
            [clojure.string :as s]
            [org.akvo.flow-api.anomaly :as anomaly]
            [org.akvo.flow-api.datastore :as ds]
            [org.akvo.flow-api.utils :as utils])
  (:refer-clojure :exclude [list])
  (:import [com.fasterxml.jackson.core JsonParseException]
           [com.google.appengine.api.datastore Entity Text QueryResultIterator QueryResultIterable]
           [java.text SimpleDateFormat]))

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
(defmulti parse-response (fn [type response-str opts] type))

(defmethod parse-response "FREE_TEXT"
  [_ response-str opts]
  response-str)

(defmethod parse-response "OPTION"
  [_ response-str opts]
  (try (json/parse-string response-str)
       (catch JsonParseException _)))

(defn parse-double [s]
  (try (Double/parseDouble s)
       (catch NumberFormatException _)))

(defmethod parse-response "NUMBER"
  [_ response-str opts]
  (parse-double response-str))

;; 52.40376391|-1.75630525|189.6|6oqmgtjv
(defmethod parse-response "GEO"
  [_ response-str opts]
  (let [[lat long elev code] (s/split response-str #"\|")]
    (when-not (and (nil? lat) (nil? long))
      {:lat (parse-double lat)
       :long (parse-double long)
       :elev (when elev (parse-double elev))
       :code code})))

(defn replace-path [response-str asset-url-root]
  (if (nil? asset-url-root)
    response-str
    (str (utils/ensure-trailing-slash asset-url-root)
         (last (s/split response-str #"/")))))

;; {"filename":"/storage/.../.jpg","location":null}
;; or
;; /storage/.../.jpg
;; TODO: If location is non-null, what is the format?
(defmethod parse-response "PHOTO"
  [_ response-str {:keys [asset-url-root]}]
  (let [photo (try (json/parse-string response-str)
                   (catch JsonParseException _
                     {"filename" response-str
                      "location" nil}))]
    (when (map? photo)
      (update photo "filename" replace-path asset-url-root))))

(defmethod parse-response "VIDEO"
  [_ response-str {:keys [asset-url-root]}]
  (let [video (try (json/parse-string response-str)
                   (catch JsonParseException _
                     {"filename" response-str
                      "location" nil}))]
    (when (map? video)
      (update video "filename" replace-path asset-url-root)) ))

(defmethod parse-response "SCAN"
  [_ response-str opts]
  response-str)

(defmethod parse-response "TRACK"
  [_ response-str opts]
  response-str)

(defmethod parse-response "NAME"
  [_ response-str opts]
  response-str)

(defmethod parse-response "STRENGTH"
  [_ response-str opts]
  response-str)

(defmethod parse-response "DATE"
  [_ response-str opts]
  (let [date (try (java.util.Date. (Long/parseLong response-str))
                  (catch NumberFormatException e
                    (let [;; SimpleDateFormat is not thread safe so we create a
                          ;; new one every time.
                          date-format (SimpleDateFormat. "dd-MM-yyyy HH:mm:ss z")]
                      (.parse date-format response-str))))]
    (ds/to-iso-8601 date)))

(defmethod parse-response "CASCADE"
  [_ response-str opts]
  (try (json/parse-string response-str)
       (catch JsonParseException _)))

(defmethod parse-response "GEOSHAPE"
  [_ response-str opts]
  (try (json/parse-string response-str)
       (catch JsonParseException _)))

(defmethod parse-response "SIGNATURE"
  [_ response-str opts]
  (try (json/parse-string response-str)
       (catch JsonParseException _)))

(defmethod parse-response "CADDISFLY"
  [_ response-str opts]
  (json/parse-string response-str))

(defn response-entity->map [^Entity response]
  {:form-instance-id (str (.getProperty response "surveyInstanceId"))
   :question-id (.getProperty response "questionID")
   :response-str (or (.getProperty response "value")
                     (.getValue ^Text (.getProperty response "valueText")))
   :iteration (or (.getProperty response "iteration") 0)})

(defn update-form-instances-fn [question-types opts]
  (fn [form-instances ^Entity response]
    (let [{:keys [form-instance-id
                  question-id
                  response-str
                  iteration]} (response-entity->map response)]
      (if-let [question-type (get question-types question-id)]
        (let [response (when response-str
                         (parse-response question-type
                                         response-str
                                         opts))]
          (assoc-in form-instances
                    [form-instance-id
                     question-id
                     iteration]
                    response))
        form-instances))))

(defn fetch-answers [ds form-definition form-instances opts]
  (let [question-types (question-type-map form-definition)]
    (reduce (fn [form-instance-answers form-instance-batch]
              (reduce (update-form-instances-fn question-types opts)
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
  {:id (str (ds/id form-instance))
   :form-id (str (.getProperty form-instance "surveyId"))
   :surveyal-time (.getProperty form-instance "surveyalTime")
   :submitter (.getProperty form-instance "submitterName")
   :submission-date (.getProperty form-instance "collectionDate")
   :device-identifier (.getProperty form-instance "deviceIdentifier")
   :data-point-id (str (.getProperty form-instance "surveyedLocaleId"))
   :identifier (.getProperty form-instance "surveyedLocaleIdentifier")
   :display-name (.getProperty form-instance "surveyedLocaleDisplayName")
   :created-at (ds/created-at form-instance)
   :modified-at (ds/modified-at form-instance)})

(defn list
  ([ds form-definition]
   (list ds form-definition {}))
  ([ds form-definition opts]
   (let [form-instances-iterator (form-instances-query ds form-definition opts)
         form-instances (mapv form-instance-entity->map (iterator-seq form-instances-iterator))
         cursor (let [cursor (.toWebSafeString (.getCursor form-instances-iterator))]
                  (when-not (empty? cursor)
                    cursor))
         answers (fetch-answers ds form-definition form-instances opts)]
     {:form-instances (mapv (fn [form-instance]
                              (assoc form-instance
                                     :responses
                                     (get answers (:id form-instance))))
                            form-instances)
      :cursor cursor})))
