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
