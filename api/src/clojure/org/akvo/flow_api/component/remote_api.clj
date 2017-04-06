(ns org.akvo.flow-api.component.remote-api
  (:require [com.stuartsierra.component :as component]))

(defrecord RemoteApi []
  component/Lifecycle
  (start [this] this)
  (stop [this] this))

(defn remote-api [_]
  (->RemoteApi))

(defrecord LocalApi []
  component/Lifecycle
  (start [this] this)
  (stop [this] this))

(defn local-api [_]
  (->LocalApi))
