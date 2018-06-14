#!/usr/bin/env bash

set -eu


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
       --volume "$PWD/nginx/:/conf" \
       --entrypoint /usr/local/openresty/bin/openresty \
       openresty/openresty:1.11.2.3-alpine-fat -t -c /conf/nginx.conf
(
    cd nginx
    docker build -t "${PROXY_IMAGE_NAME:=akvo/flow-api-proxy}" .
)


docker-compose -p akvo-flow-api-ci -f docker-compose.yml -f docker-compose.ci.yml run --no-deps tests dev/run-as-user.sh lein with-profile +assemble  do jar, assemble

(
    cd api
    docker build -t "${BACKEND_IMAGE_NAME:=akvo/flow-api-backend}" .
)
