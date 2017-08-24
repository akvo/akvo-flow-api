#!/usr/bin/env bash

set -eu

CACHE=".cache"

if [[ ! -d "${HOME}/${CACHE}" ]]; then
    mkdir "${HOME}/${CACHE}"
fi

if [[ ! -d "${CACHE}" ]]; then
    mkdir "${CACHE}"
fi

if [[ "${OSTYPE}" == "linux-gnu" ]]; then
    sudo mount --bind "${HOME}/${CACHE}" "${CACHE}"
fi

cd "${CACHE}"

FILE_LIST=(https://www-eu.apache.org/dist/maven/binaries/apache-maven-3.0.5-bin.zip \
	   https://www-eu.apache.org/dist/ant/binaries/apache-ant-1.10.1-bin.zip \
	   https://storage.googleapis.com/appengine-sdks/featured/appengine-java-sdk-1.9.50.zip
	   https://s3-eu-west-1.amazonaws.com/akvoflow/test-data/local_db.bin)

for i in "${FILE_LIST[@]}"; do
    wget --quiet --timestamping "${i}"
done

if [[ ! -d "akvo-flow" ]]; then
    git clone https://github.com/akvo/akvo-flow.git
fi
