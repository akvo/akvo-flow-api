#!/usr/bin/env bash

set -eu

docker compose -p akvo-flow-api-ci -f docker-compose.yml -f docker-compose.ci.yml down -v -t 1
rm -rf api/target/*
