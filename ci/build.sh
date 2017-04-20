#!/usr/bin/env bash

set -eu

BRANCH_NAME="${TRAVIS_BRANCH:=unknown}"
LOCAL_TEST_DATA_PATH="target/stub-server-1.0-SNAPSHOT/WEB-INF/appengine-generated"

# Flow data access classes, only if not present in $HOME/.m2

DATA_ACCESS_VERSION=$(sed -n -e 's|.*org\.akvo\.flow/data-access "\(.*\)"]$|\1|p' api/project.clj)

if [[ ! -f "${HOME}/.m2/repository/org/akvo/flow/data-access/${DATA_ACCESS_VERSION}/data-access-${DATA_ACCESS_VERSION}.jar" ]]; then
    cd flow
    ./build.sh
    cd ..
fi

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

# Build docker images if branch is `develop`

if [[ "${BRANCH_NAME}" != "develop" ]]; then
    echo "Skipping docker build"
    exit 0
fi

cd nginx

docker build -t "${PROXY_IMAGE_NAME:=akvo/flow-api-proxy}" .

cd ..

cd api

lein uberjar

find "${HOME}/.m2" \
     \( \
     -name 'datanucleus-core-1.1.5.jar' -or \
     -name 'datanucleus-jpa-1.1.5.jar' -or \
     -name 'datanucleus-appengine-1.0.10.final.jar' \
     \) \
     -exec cp -v {} target/uberjar/ \;

docker build -t "${BACKEND_IMAGE_NAME:=akvo/flow-api-backend}" .
