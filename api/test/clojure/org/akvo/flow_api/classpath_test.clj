(ns org.akvo.flow-api.classpath-test
  "`appengine-remote-api` repackages classes compiled against an unshaded
  `com.google.gson` that its pom never declares. `appengine-api-1.0-sdk` ships the
  same class names compiled against the gson it repackages, so whichever jar is
  searched first decides whether the missing one is ever asked for. Calling that
  class from a test proves nothing -- Leiningen's order picks the copy that works,
  which is how the suite stayed green while the deployed container raised
  `NoClassDefFoundError: com/google/gson/stream/JsonWriter` on every request.
  Asserting the unshaded class is present is what discriminates."
  (:require [clojure.test :refer [deftest is]]))

(deftest unshaded-gson-is-on-the-classpath
  (is (Class/forName "com.google.gson.stream.JsonWriter")))
