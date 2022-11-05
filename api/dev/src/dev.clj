(ns dev
  (:refer-clojure :exclude [test])
  (:require [clojure.java.io :as io]
            [clojure.pprint :refer [pprint]]
            [clojure.repl :refer :all]
            [clojure.tools.namespace.repl :refer [refresh refresh-all]]
            [com.stuartsierra.component :as component]
            [duct.generate :as gen]
            [eftest.runner :as eftest]
            [duct.util.repl :refer [setup cljs-repl migrate rollback]]
            [duct.util.system :refer [load-system]]
            [reloaded.repl :refer [system init start stop go reset]]))

(defn new-system []
  (load-system (keep io/resource ["org/akvo/flow_api/system.edn" "dev.edn" "local.edn"])))

(defn test []
  (eftest/run-tests (->> (eftest/find-tests "test")
                         (remove (fn [t] (or (-> t meta :kubernetes-test)
                                             (-> t meta :ns meta :kubernetes-test)))))
                    {:fail-fast? true}))

(when (io/resource "local.clj")
  (load "local"))

(gen/set-ns-prefix 'org.akvo.flow-api)

(reloaded.repl/set-init! new-system)



(comment

  (reset)
  (test)

  ,)
