(ns org.akvo.flow-api.component.akvo-flow-server-config
  (:require [com.stuartsierra.component :as component]
            [org.akvo.flow-api.akvo-flow-server-config :as afsc]))

(defrecord AkvoFlowServerConfig [github-auth-token tmp-dir]
  component/Lifecycle
  (start [this]
    (let [instances (afsc/get-instance-map github-auth-token tmp-dir)
          aliases (afsc/get-alias-map instances)]
      (assoc this
             :instances instances
             :aliases aliases)))
  (stop [this] this))

(defn akvo-flow-server-config [{:keys [github-auth-token tmp-dir]}]
  (->AkvoFlowServerConfig github-auth-token tmp-dir))

(defrecord DummyAkvoFlowServerConfig []
  component/Lifecycle
  (start [this] this)
  (stop [this] this))

(defn dummy-akvo-flow-server-config [_]
  (->DummyAkvoFlowServerConfig))
