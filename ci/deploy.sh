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

ENVIRONMENT=test
if [[ "${TRAVIS_BRANCH}" == "master" ]]; then
    gcloud container clusters get-credentials production
    CONFIG_MAP=prod
    ENVIRONMENT=production
    BACKEND_POD_CPU_REQUESTS="200m"
    BACKEND_POD_CPU_LIMITS="400m"
    BACKEND_POD_MEM_REQUESTS="512Mi"
    BACKEND_POD_MEM_LIMITS="1024Mi"
    PROXY_POD_CPU_REQUESTS="100m"
    PROXY_POD_CPU_LIMITS="200m"
    PROXY_POD_MEM_REQUESTS="16Mi"
    PROXY_POD_MEM_LIMITS="32Mi"
else
    gcloud container clusters get-credentials test
    CONFIG_MAP=dev
    BACKEND_POD_CPU_REQUESTS="100m"
    BACKEND_POD_CPU_LIMITS="200m"
    BACKEND_POD_MEM_REQUESTS="256Mi"
    BACKEND_POD_MEM_LIMITS="384Mi"
    PROXY_POD_CPU_REQUESTS="50m"
    PROXY_POD_CPU_LIMITS="100m"
    PROXY_POD_MEM_REQUESTS="16Mi"
    PROXY_POD_MEM_LIMITS="32Mi"
fi

# Deploying

sed -e "s/\${ENVIRONMENT}/${ENVIRONMENT}/" \
  -e "s/\${BACKEND_POD_CPU_REQUESTS}/${BACKEND_POD_CPU_REQUESTS}/" \
  -e "s/\${BACKEND_POD_MEM_REQUESTS}/${BACKEND_POD_MEM_REQUESTS}/" \
  -e "s/\${BACKEND_POD_CPU_LIMITS}/${BACKEND_POD_CPU_LIMITS}/" \
  -e "s/\${BACKEND_POD_MEM_LIMITS}/${BACKEND_POD_MEM_LIMITS}/" \
  -e "s/\${PROXY_POD_CPU_REQUESTS}/${PROXY_POD_CPU_REQUESTS}/" \
  -e "s/\${PROXY_POD_MEM_REQUESTS}/${PROXY_POD_MEM_REQUESTS}/" \
  -e "s/\${PROXY_POD_CPU_LIMITS}/${PROXY_POD_CPU_LIMITS}/" \
  -e "s/\${PROXY_POD_MEM_LIMITS}/${PROXY_POD_MEM_LIMITS}/" \
  ci/k8s/deployment.yaml.template > ci/k8s/deployment.yaml

kubectl apply -f ci/k8s/deployment.yml
kubectl apply -f ci/k8s/service.yml
kubectl apply -f "ci/k8s/${CONFIG_MAP}/config-map.yml"
