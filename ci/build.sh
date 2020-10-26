#!/usr/bin/env bash

set -eu

function log {
   echo "$(date +"%T") - INFO - $*"
}

if [ -z "$TRAVIS_COMMIT" ]; then
    export TRAVIS_COMMIT=local
fi

if [[ "${TRAVIS_TAG:-}" =~ promote-.* ]]; then
    echo "Skipping build as it is a prod promotion"
    exit 0
fi

log Login to Docker
echo "$DOCKER_PASSWORD" | docker login --username "$DOCKER_USERNAME" --password-stdin

LOCAL_TEST_DATA_PATH="gae-dev-server/target/stub-server-1.0-SNAPSHOT/WEB-INF/appengine-generated"

mkdir -p "$HOME/.m2/repository"

mkdir -p "${LOCAL_TEST_DATA_PATH}"

if ! [ -f "${HOME}/.cache/local_db.bin" ]; then
    wget -q "https://s3-eu-west-1.amazonaws.com/akvoflow/test-data/local_db.bin" -O "${HOME}/.cache/local_db.bin"
fi

cp -v "${HOME}/.cache/local_db.bin" "${LOCAL_TEST_DATA_PATH}"

(
    log Building Keycloak nginx proxy
    cd nginx
    docker build \
	   -t "akvo/flow-api-proxy:latest" \
	   -t "akvo/flow-api-proxy:${TRAVIS_COMMIT}" .

    log Check nginx configuration
    docker run \
       --rm \
       --entrypoint /usr/local/openresty/bin/openresty \
       "akvo/flow-api-proxy" -t -c /usr/local/openresty/nginx/conf/nginx.conf

    log Test KC based auth
    docker-compose up -d
    docker-compose exec testnetwork /bin/sh -c 'cd /usr/local/src/ && ./entrypoint.sh ./test-auth.sh'
    docker-compose down -v
)



(
    cd nginx-auth0
    log Building Auth0 nginx proxy
    docker build \
	   -t "akvo/flow-api-auth0-proxy:latest" \
	   -t "akvo/flow-api-auth0-proxy:${TRAVIS_COMMIT}" .

    log Check Auth0 nginx configuration
    docker run \
	   --rm \
	   --entrypoint /usr/local/openresty/bin/openresty \
	   "akvo/flow-api-auth0-proxy" -t -c /usr/local/openresty/nginx/conf/nginx.conf

    log Test Auth0 based auth
    docker-compose up -d
    docker-compose exec testnetwork /bin/sh -c 'cd /usr/local/src/ && ./entrypoint.sh ./test-cache.sh'
    docker-compose down -v
)

log Starting Backend tests docker environment
docker-compose -p akvo-flow-api-ci -f docker-compose.yml -f docker-compose.ci.yml up --build -d
log Starting tests
docker-compose -p akvo-flow-api-ci -f docker-compose.yml -f docker-compose.ci.yml run --no-deps tests dev/run-as-user.sh lein do clean, check, eastwood, test :all
log Building uberjar
docker-compose -p akvo-flow-api-ci -f docker-compose.yml -f docker-compose.ci.yml run --no-deps tests dev/run-as-user.sh lein with-profile +assemble  do jar, assemble

log Building final container
(
    cd api
    docker build \
	   -t "akvo/flow-api-backend" \
	   -t "akvo/flow-api-backend:$TRAVIS_COMMIT" .
)

log Done