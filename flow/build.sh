#!/usr/bin/env bash

set -eu

./get-dependencies.sh

docker pull akvo/flow-api-build

docker run --rm \
       --volume "$PWD/.cache/akvo-flow":/opt/akvo-flow \
       --volume "$HOME/.m2/repository":/opt/.m2/repository \
       --user `id -u`:`id -g` \
       akvo/flow-api-build
