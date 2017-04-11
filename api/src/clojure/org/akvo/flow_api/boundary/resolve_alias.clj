(ns org.akvo.flow-api.boundary.resolve-alias
  (:refer-clojure :exclude [resolve])
  (:require [org.akvo.flow-api.component.akvo-flow-server-config])
  (:import [org.akvo.flow_api.component.akvo_flow_server_config AkvoFlowServerConfig DummyAkvoFlowServerConfig]))

(defprotocol IResolveAlias
  (resolve [this alias] "Resolve the akvo flow instance alias to an instance-id. Return nil if alias is not found."))

(extend-protocol IResolveAlias
  AkvoFlowServerConfig
  (resolve [{:keys [instances aliases]} alias]
    (if (contains? instances alias)
      alias
      (get aliases alias)))
  DummyAkvoFlowServerConfig
  (resolve [_ alias]
    alias))

(defn alias-resolver
  "Takes an argument that implements IResolveAlias and returns a function that will
  resolve an alias or throw an exception if the alias is not found"
  [resolver]
  (fn [alias]
    (if-let [instance-id (resolve resolver alias)]
      instance-id
      (throw (ex-info (format "Alias %s not found" alias)
                      {:status :not-found
                       :alias alias})))))
