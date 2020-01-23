(ns org.akvo.flow-api.component.akvo-flow-server-config
  (:require [com.stuartsierra.component :as component]
            [org.akvo.flow-api.akvo-flow-server-config :as afsc]
            [clojure.java.io :as io])
  (:import [org.apache.commons.io FileUtils]))

(defn delete-old-tmp-dir [tmp-subdir]
  (when tmp-subdir
    (future
      (Thread/sleep 60000)
      (FileUtils/deleteDirectory (io/file tmp-subdir)))))

(defn- flow-config-map [github-auth-token tmp-dir]
  (let [new-tmp-subdir (str tmp-dir "/" (System/currentTimeMillis) "/")
        instances (afsc/get-instance-map github-auth-token new-tmp-subdir)
        aliases (afsc/get-alias-map instances)]
    {:tmp-subdir new-tmp-subdir
     :instances instances
     :aliases   aliases}))

(defn refresh! [{:keys [flow-config github-auth-token tmp-dir]}]
  (let [old-tmp-dir (:tmp-subdir @flow-config)]
    (reset! flow-config (flow-config-map github-auth-token tmp-dir))
    (delete-old-tmp-dir old-tmp-dir)))

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
  (start [this]
    (assoc this
           :flow-config (atom {:instances {"akvoflowsandbox" "akvoflowsandbox"}
                               :aliases {"aquaforall" "akvoflow-70"
                                         "uat1" "akvoflow-uat1"
                                         "uat2" "akvoflow-uat2"}})))
  (stop [this] this))

(defn dummy-akvo-flow-server-config [_]
  (->DummyAkvoFlowServerConfig))


(defrecord LocalAkvoFlowServerConfig [config-folder instance-id alias]
  component/Lifecycle
  (start [this]
    (let [instance-props (afsc/read-instance-props (str config-folder "/" instance-id)
                                                   "appengine-web.xml")]
      (assoc this :flow-config (atom {:instances {instance-id instance-props}
                                      :aliases {alias instance-id}}))))
  (stop [this]
    this))

(defn local-akvo-flow-server-config [{:keys [config-folder instance-id alias]}]
  (->LocalAkvoFlowServerConfig config-folder instance-id alias))
