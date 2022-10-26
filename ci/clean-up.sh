#!/usr/bin/env bash

set -eu

docker compose -p akvo-flow-api-ci -f docker-compose.yml -f docker-compose.ci.yml down -v
rm -rf api/target/*
