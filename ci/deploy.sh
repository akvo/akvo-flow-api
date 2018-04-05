#!/usr/bin/env bash

set -eu

if [[ "${TRAVIS_BRANCH}" != "develop" ]] && [[ "${TRAVIS_BRANCH}" != "master" ]]; then
    exit 0
fi

if [[ "${TRAVIS_PULL_REQUEST}" != "false" ]]; then
    exit 0
fi

# Pushing images
docker login -u="${DOCKER_USERNAME}" -p="${DOCKER_PASSWORD}"
docker push "${PROXY_IMAGE_NAME:=akvo/flow-api-proxy}"
docker push "${BACKEND_IMAGE_NAME:=akvo/flow-api-backend}"

# Making sure gcloud and kubectl are installed and up to date
gcloud components install kubectl
gcloud components update
gcloud version
which gcloud kubectl

# Authentication with gcloud and kubectl
gcloud auth activate-service-account --key-file ci/gcloud-service-account.json
gcloud config set project akvo-lumen
gcloud config set container/cluster europe-west1-d
gcloud config set compute/zone europe-west1-d

if [[ "${TRAVIS_BRANCH}" == "master" ]]; then
    gcloud container clusters get-credentials production
    CONFIG_MAP=prod
else
    gcloud container clusters get-credentials test
    CONFIG_MAP=dev
fi

# Deploying

kubectl apply -f ci/k8s/deployment.yml
kubectl apply -f ci/k8s/service.yml
kubectl apply -f "ci/k8s/${CONFIG_MAP}/config-map.yml"
