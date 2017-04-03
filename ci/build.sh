#!/usr/bin/env bash

set -eu

TEST_DATA_URL="https://s3-eu-west-1.amazonaws.com/akvoflow/test-data/local_db.bin"
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

curl -Lo "${LOCAL_TEST_DATA_PATH}/local_db.bin" "${TEST_DATA_URL}"

mvn appengine:devserver_start

# Check API code

cd ../api

lein do clean, check, test :all, eastwood '{:source-paths ["src" "test"]}'

cd ../gae-dev-server

mvn appengine:devserver_stop

cd ..
