#!/usr/bin/env bash

set -eu

if [ -z "$TRAVIS_COMMIT" ]; then
    export TRAVIS_COMMIT=local
fi

if [[ "${TRAVIS_TAG:-}" =~ promote-.* ]]; then
    echo "Skipping build as it is a prod promotion"
    exit 0
fi

LOCAL_TEST_DATA_PATH="gae-dev-server/target/stub-server-1.0-SNAPSHOT/WEB-INF/appengine-generated"

mkdir -p "$HOME/.m2/repository"

mkdir -p "${LOCAL_TEST_DATA_PATH}"

if ! [ -f "${HOME}/.cache/local_db.bin" ]; then
    wget -q "https://s3-eu-west-1.amazonaws.com/akvoflow/test-data/local_db.bin" -O "${HOME}/.cache/local_db.bin"
fi

cp -v "${HOME}/.cache/local_db.bin" "${LOCAL_TEST_DATA_PATH}"

(
    cd nginx
    docker build \
	   -t "akvo/flow-api-proxy:latest" \
	   -t "akvo/flow-api-proxy:${TRAVIS_COMMIT}" .
)

# Check nginx configuration
docker run \
       --rm \
       --entrypoint /usr/local/openresty/bin/openresty \
       "akvo/flow-api-proxy" -t -c /usr/local/openresty/nginx/conf/nginx.conf

(
    cd nginx-auth0
    docker build \
	   -t "akvo/flow-api-auth0-proxy:latest" \
	   -t "akvo/flow-api-auth0-proxy:${TRAVIS_COMMIT}" .
)

# Check nginx auth0 configuration
docker run \
       --rm \
       --entrypoint /usr/local/openresty/bin/openresty \
       "akvo/flow-api-auth0-proxy" -t -c /usr/local/openresty/nginx/conf/nginx.conf

# Checking nginx caching
(
    cd nginx-auth0
    docker-compose up -d
    sleep 5
    ./test.sh "http://localhost:8082/flow/ok" 2>&1 | grep 'X-Cache-Status: MISS'
    ./test.sh "http://localhost:8082/flow/" 2>&1 | grep 'X-Cache-Status: MISS'
    ./test.sh "http://localhost:8082/flow/" 2>&1 | grep 'X-Cache-Status: HIT'
    ./test.sh "http://localhost:8082/flow/" 2>&1 | grep 'X-Cache-Status: HIT'
    docker-compose down -v
)

# Backend tests
docker-compose -p akvo-flow-api-ci -f docker-compose.yml -f docker-compose.ci.yml up --build -d
docker-compose -p akvo-flow-api-ci -f docker-compose.yml -f docker-compose.ci.yml run --no-deps tests dev/run-as-user.sh lein do clean, check, eastwood, test :all
docker-compose -p akvo-flow-api-ci -f docker-compose.yml -f docker-compose.ci.yml run --no-deps tests dev/run-as-user.sh lein with-profile +assemble  do jar, assemble

(
    cd api
    docker build \
	   -t "akvo/flow-api-backend" \
	   -t "akvo/flow-api-backend:$TRAVIS_COMMIT" .
)
