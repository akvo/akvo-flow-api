#!/usr/bin/env bash

set -eu

if [ -z "$TRAVIS_COMMIT" ]; then
    export TRAVIS_COMMIT=local
fi

if [[ "${TRAVIS_TAG:-}" =~ promote-.* ]]; then
    log "Skipping build as it is a prod promotion"
    exit 0
fi

LOCAL_TEST_DATA_PATH="gae-dev-server/target/stub-server-1.0-SNAPSHOT/WEB-INF/appengine-generated"

mkdir -p "$HOME/.m2/repository"

mkdir -p "${LOCAL_TEST_DATA_PATH}"

if ! [ -f "${HOME}/.cache/local_db.bin" ]; then
    wget -q "https://s3-eu-west-1.amazonaws.com/akvoflow/test-data/local_db.bin" -O "${HOME}/.cache/local_db.bin"
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
    docker build -t "akvo/flow-api-proxy" .
    docker tag akvo/flow-api-proxy akvo/flow-api-proxy:$TRAVIS_COMMIT
)


# Check nginx auth0 configuration

(
    cd nginx-auth0
    docker build -t "akvo/flow-api-auth0-proxy" .
    docker tag akvo/flow-api-auth0-proxy akvo/flow-api-auth0-proxy:$TRAVIS_COMMIT
)

docker run \
       --rm \
       --entrypoint /usr/local/openresty/bin/openresty \
       akvo/flow-api-auth0-proxy -t -c /usr/local/openresty/nginx/conf/nginx.conf

docker-compose -p akvo-flow-api-ci -f docker-compose.yml -f docker-compose.ci.yml run --no-deps tests dev/run-as-user.sh lein with-profile +assemble  do jar, assemble

(
    cd api
    docker build -t "akvo/flow-api-backend" .
    docker tag akvo/flow-api-backend akvo/flow-api-backend:$TRAVIS_COMMIT
)
