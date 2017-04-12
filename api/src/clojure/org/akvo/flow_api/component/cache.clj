(ns org.akvo.flow-api.component.cache
  (:require [clojure.core.cache :as cache]
            [com.stuartsierra.component :as component]))

(defrecord TTLMemoryCache [ttl]
  component/Lifecycle
  (start [this]
    (assoc this :cache (atom (cache/ttl-cache-factory {} :ttl ttl))))
  (stop [this]
    (assoc this :cache nil)))

(defn ttl-memory-cache [{:keys [ttl]}]
  (->TTLMemoryCache ttl))
