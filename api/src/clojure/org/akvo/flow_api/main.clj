(ns org.akvo.flow-api.main
  (:gen-class)
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [com.stuartsierra.component :as component]
            [duct.util.runtime :refer [add-shutdown-hook]]
            [duct.util.system :refer [load-system]]
            [environ.core :refer [env]]
            [nrepl.server :as repl]
            [org.akvo.flow-api.utils :as utils])
  (:import (java.io FileNotFoundException)))

(defn secret-value
  "Reads the value of a secret from a file. It expects an
  environment variable SECRETS_MOUNT_PATH pointing to a folder
  containing secret files"
  [secret-key]
  (let [mount-path (utils/ensure-trailing-slash (:secrets-mount-path env))]
    (-> (format "%s%s" mount-path secret-key)
        slurp
        str/trim)))

(defn maybe-secret-value
  "Reads the value of a secret from a file. It expects an
  environment variable SECRETS_MOUNT_PATH pointing to a folder
  containing secret files"
  [secret-key]
  (try
    (secret-value secret-key)
    (catch FileNotFoundException _ nil)))

(defn -main [& args]
  (let [bindings {'http-port (Integer/parseInt (:http-port env "3000"))
                  'github-auth-token (secret-value "github-auth-token")
                  'sentry-dsn (secret-value "sentry-dsn")
                  'tmp-dir (utils/ensure-trailing-slash (System/getProperty "java.io.tmpdir"))
                  'event-log-user (secret-value "event-log-user")
                  'event-log-password (secret-value "event-log-password")
                  'cloud-sql-instance (maybe-secret-value "cloud-sql-instance")
                  'event-log-host (secret-value "event-log-host")
                  'event-log-port (Integer/parseInt (secret-value "event-log-port"))}
        system   (->> (load-system [(io/resource "org/akvo/flow_api/system.edn")] bindings)
                      (component/start))]
    (add-shutdown-hook ::stop-system #(component/stop system))
    (println "Started HTTP server on port" (-> system :http :port))
    (let [repl-server (repl/start-server :port (Integer/parseInt (:repl-port env "0")))]
      (println "REPL started on port" (:port repl-server)))))
