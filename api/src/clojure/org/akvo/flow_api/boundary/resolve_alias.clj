(ns org.akvo.flow-api.boundary.resolve-alias
  (:refer-clojure :exclude [resolve])
  (:require [org.akvo.flow-api.component.akvo-flow-server-config]))

(defn resolve [{:keys [flow-config]} alias]
  (let [{:keys [instances aliases]} @flow-config]
    (if (contains? instances alias)
      alias
      (get aliases alias))))