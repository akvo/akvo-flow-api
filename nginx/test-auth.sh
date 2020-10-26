#!/usr/bin/env bash

set -eu

wait4ports api-proxy=tcp://localhost:8081 upstream=tcp://localhost:3000

