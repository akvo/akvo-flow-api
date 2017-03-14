#!/usr/bin/env bash

set -eu

CACHE=".akvo-flow-cache"

mkdir -p "${CACHE}"

cd "${CACHE}"

FILE_LIST=(https://www-eu.apache.org/dist/maven/binaries/apache-maven-3.0.5-bin.zip \
	   https://www-eu.apache.org/dist/ant/binaries/apache-ant-1.10.1-bin.zip \
	   https://storage.googleapis.com/appengine-sdks/featured/appengine-java-sdk-1.9.50.zip)

for i in "${FILE_LIST[@]}"; do
    if [[ ! -f $(basename "${i}") ]]; then
	wget "${i}"
    fi
done

if [[ ! -d "akvo-flow" ]]; then
    git clone https://github.com/akvo/akvo-flow.git
fi
