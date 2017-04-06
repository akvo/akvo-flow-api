(ns org.akvo.flow-api.fixtures
  (:require [clojure.test :refer :all]
            [com.stuartsierra.component :as component]
            [duct.util.system :as duct]))

(def ^:dynamic *system*)

(defn system [system]
  (fn [f]
    (binding [*system* (component/start (duct/build-system system))]
      (f)
      (component/stop system))))
