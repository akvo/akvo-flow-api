#!/usr/bin/env bash

set -eu

LOCAL_TEST_DATA_PATH="target/stub-server-1.0-SNAPSHOT/WEB-INF/appengine-generated"

# Flow data access classes

cd flow

./build.sh

cd ..

# Lein

mkdir -p $HOME/.lein
echo '{:user {:plugins [[jonase/eastwood "0.2.3"]]}}' > $HOME/.lein/profiles.clj

# GAE dev server test data

cd gae-dev-server

mvn clean install

mkdir -p "${LOCAL_TEST_DATA_PATH}"

cp -v "${HOME}/.cache/local_db.bin" "${LOCAL_TEST_DATA_PATH}"

mvn appengine:devserver_start

# Check API code

cd ../api

lein do clean, check, test :all, eastwood '{:source-paths ["src/clojure" "test/clojure"]}'

cd ../gae-dev-server

mvn appengine:devserver_stop

cd ..
