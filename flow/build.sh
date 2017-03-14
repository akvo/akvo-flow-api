#!/usr/bin/env bash

docker run --rm --interactive --tty \
       --volume "$PWD/.akvo-flow-cache/akvo-flow":/opt/akvo-flow \
       --volume "$HOME/.m2/repository":/opt/.m2/repository \
       --user `id -u`:`id -g` \
       akvo/flow-dev
