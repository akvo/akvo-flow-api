(ns org.akvo.flow-api.component.akvo-flow-server-config
  (:require [com.stuartsierra.component :as component]
            [org.akvo.flow-api.akvo-flow-server-config :as afsc]))

(defn- flow-config-map [github-auth-token tmp-dir]
  (let [new-tmp-subdir (str tmp-dir "/" (System/currentTimeMillis) "/")
        instances (afsc/get-instance-map github-auth-token new-tmp-subdir)
        aliases (afsc/get-alias-map instances)]
    {:tmp-subdir new-tmp-subdir
     :instances instances
     :aliases   aliases}))

(defn refresh! [{:keys [flow-config github-auth-token tmp-dir]}]
  (reset! flow-config (flow-config-map github-auth-token tmp-dir)))

(defrecord AkvoFlowServerConfig [github-auth-token tmp-dir]
  component/Lifecycle
  (start [this]
    (assoc this
      :flow-config (atom (flow-config-map github-auth-token tmp-dir))))
  (stop [this] this))

(defn akvo-flow-server-config [{:keys [github-auth-token tmp-dir]}]
  (->AkvoFlowServerConfig github-auth-token tmp-dir))

(defrecord DummyAkvoFlowServerConfig []
  component/Lifecycle
  (start [this] this)
  (stop [this] this))

(defn dummy-akvo-flow-server-config [_]
  (->DummyAkvoFlowServerConfig))
