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
    gcloud container clusters get-credentials lumen
else
    gcloud container clusters get-credentials dev-cluster
fi

# Deploying

kubectl delete -f ci/k8s/deployment.yml
kubectl create -f ci/k8s/deployment.yml
