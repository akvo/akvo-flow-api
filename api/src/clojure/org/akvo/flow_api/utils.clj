(ns org.akvo.flow-api.utils
  (:require [clojure.string :as s]))

(defn kebab->camel [kw]
  (if (keyword? kw)
    (let [parts (s/split (name kw) #"\-")]
      (apply str (first parts) (map s/capitalize (rest parts))))
    kw))
