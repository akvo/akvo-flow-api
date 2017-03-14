#!/usr/bin/env bash

set -eu

./get-dependencies.sh

IMAGE=$(docker images | grep 'akvo/flow-build' || echo -n "")

if [[ -z "${IMAGE}" ]]; then
    docker build -t akvo/flow-build .
fi

docker run --rm \
       --volume "$PWD/.akvo-flow-cache/akvo-flow":/opt/akvo-flow \
       --volume "$HOME/.m2/repository":/opt/.m2/repository \
       --user `id -u`:`id -g` \
       akvo/flow-build
