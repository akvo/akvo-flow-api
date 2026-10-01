(ns org.akvo.flow-api.middleware.anomaly
  (:require [org.akvo.flow-api.endpoint.anomaly :as anomaly]
            [org.akvo.flow-api.anomaly :as an]
            [clojure.tools.logging :as log])
  (:import [clojure.lang ExceptionInfo]))

(defn wrap-anomaly [handler]
  (fn [request]
    (try
      (handler request)
      (catch ExceptionInfo e
        (anomaly/handle e)))))

;; Matched conditions are translated quietly; each is a status the caller can act on.
;; Anything else reaches the client as a bare "Internal Server Error" from `:hide-errors`,
;; so the logged trace is the only record. It has no request context -- pair the timestamp
;; with the proxy access log for the path and org.
(defn translate-exception [^Throwable e]
  (condp #(.contains ^String %2 ^String %1) (or (.getMessage e) "")
    "Over Quota" (an/too-many-requests)
    "required more quota" (an/too-many-requests)
    "Please try again in 30 seconds" (an/bad-gateway)
    ;; Upstream failure on an idempotent read, which is what 502 says and 500 does not.
    ;; Matched on the message, not on `RemoteApiException`: that also covers credential
    ;; and configuration faults, where inviting a retry would bury a real bug.
    "remote API call: I/O error" (an/bad-gateway)
    (do (log/error e "Unhandled exception, returning 500")
        (throw e))))

(defn wrap-log-errors [handler]
  (fn [request]
    (try
      (handler request)
      (catch Throwable e
        (translate-exception e)))))
