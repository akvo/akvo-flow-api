#!/usr/bin/env bash

set -eu

BRANCH_NAME="${TRAVIS_BRANCH:=unknown}"
LOCAL_TEST_DATA_PATH="gae-dev-server/target/stub-server-1.0-SNAPSHOT/WEB-INF/appengine-generated"

mkdir -p "$HOME/.m2/repository"

mkdir -p "${LOCAL_TEST_DATA_PATH}"

if ! [ -f "${HOME}/.cache/local_db.bin" ]; then
    wget "https://s3-eu-west-1.amazonaws.com/akvoflow/test-data/local_db.bin" -O "${HOME}/.cache/local_db.bin"
fi

cp -v "${HOME}/.cache/local_db.bin" "${LOCAL_TEST_DATA_PATH}"

docker-compose -p akvo-flow-api-ci -f docker-compose.yml -f docker-compose.ci.yml up --build -d
docker-compose -p akvo-flow-api-ci -f docker-compose.yml -f docker-compose.ci.yml run --no-deps tests dev/run-as-user.sh lein do clean, check, test :all

# Check nginx configuration

docker run \
       --rm \
       --volume "$PWD/nginx/":"/conf" \
       --entrypoint /usr/local/openresty/bin/openresty \
       openresty/openresty:1.11.2.3-alpine-fat -t -c /conf/nginx.conf

# Build docker images if branch is `develop`

#if [[ "${BRANCH_NAME}" != "develop" ]] && [[ "${BRANCH_NAME}" != "master" ]]; then
#    echo "Skipping docker build"
#    exit 0
#fi

cd nginx

docker build -t "${PROXY_IMAGE_NAME:=akvo/flow-api-proxy}" .

cd ..

docker-compose -p akvo-flow-api-ci -f docker-compose.yml -f docker-compose.ci.yml run --no-deps tests dev/setup-dev-user-in-container.sh 'lein uberjar'

cd api

docker build -t "${BACKEND_IMAGE_NAME:=akvo/flow-api-backend}" .
