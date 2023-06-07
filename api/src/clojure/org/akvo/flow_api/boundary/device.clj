(ns org.akvo.flow-api.boundary.device
  (:refer-clojure :exclude [list])
  (:require org.akvo.flow-api.component.remote-api
            [org.akvo.flow-api.datastore.device :as device]))

(defn list []
  (doall (device/list)))
