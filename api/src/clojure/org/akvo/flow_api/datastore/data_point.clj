(ns org.akvo.flow-api.datastore.data-point
  (:require [akvo.commons.gae :as gae]
            [akvo.commons.gae.query :as q]
            [cheshire.core :as json]
            [medley.core :as medley]
            [clojure.string :as s]
            [org.akvo.flow-api.anomaly :as anomaly]
            [org.akvo.flow-api.datastore :as ds])
  (:refer-clojure :exclude [list])
  (:import [com.fasterxml.jackson.core JsonParseException]
           [com.google.appengine.api.datastore Entity Text QueryResultIterator QueryResultIterable Key KeyFactory DatastoreService]))

(set! *warn-on-reflection* true)

(defn data-points-query
  ^com.google.appengine.api.datastore.QueryResultIterator
  [ds survey-id {:keys [cursor page-size]}]
  (let [page-size (ds/normalize-page-size page-size)]
    (.iterator ^QueryResultIterable (q/result ds
                                              {:kind "SurveyedLocale"
                                               :filter (q/= "surveyGroupId" (Long/parseLong survey-id))}
                                              {:start-cursor cursor
                                               :chunk-size page-size
                                               :limit page-size}))))

(defn data-point-entity->map [^Entity entity]
  {:id (str (ds/id entity))
   :identifier (.getProperty entity "identifier")
   :display-name (.getProperty entity "displayName")
   :latitude (.getProperty entity "latitude")
   :longitude (.getProperty entity "longitude")
   :created-at (ds/created-at entity)
   :modified-at (ds/modified-at entity)})

(defn list
  ([ds survey]
   (list ds survey {}))
  ([ds survey opts]
   (let [data-points-iterator (data-points-query ds (:id survey) opts)
         data-points (mapv data-point-entity->map (iterator-seq data-points-iterator))
         cursor (ds/cursor data-points-iterator)]
     {:data-points data-points
      :cursor cursor})))

(defn data-points-by-id
  ^com.google.appengine.api.datastore.QueryResultIterator
  [^DatastoreService ds ids]
  (.get ds ^Iterable (mapv (fn [^Long x]
                             (KeyFactory/createKey "SurveyedLocale" x)) ids)))

(defn by-ids [ds ids]
  (mapv data-point-entity->map (vals (data-points-by-id ds ids))))
