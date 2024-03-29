#!/usr/bin/env bash

set -eu

if [[ "${CI_BRANCH}" != "develop" ]] && [[ ! "${CI_TAG:-}" =~ promote-.* ]]; then
    exit 0
fi

if [[ "${CI_PULL_REQUEST}" != "false" ]]; then
    exit 0
fi

# Authentication with gcloud and kubectl
gcloud auth activate-service-account --key-file=/home/semaphore/.secrets/gcp.json
gcloud config set project akvo-lumen
gcloud config set container/cluster europe-west1-d
gcloud config set compute/zone europe-west1-d
gcloud config set container/use_client_certificate False

ENVIRONMENT="test"
if [[ "${CI_TAG:-}" =~ promote-.* ]]; then
    gcloud container clusters get-credentials production
    CONFIG_MAP=prod
    ENVIRONMENT=production
    BACKEND_POD_CPU_REQUESTS="200m"
    BACKEND_POD_CPU_LIMITS="2000m"
    BACKEND_POD_MEM_REQUESTS="2324Mi"
    BACKEND_POD_MEM_LIMITS="2324Mi"
    PROXY_POD_CPU_REQUESTS="100m"
    PROXY_POD_CPU_LIMITS="200m"
    PROXY_POD_MEM_REQUESTS="16Mi"
    PROXY_POD_MEM_LIMITS="32Mi"
else
    gcloud container clusters get-credentials test
    CONFIG_MAP=dev
    BACKEND_POD_CPU_REQUESTS="100m"
    BACKEND_POD_CPU_LIMITS="2000m"
    BACKEND_POD_MEM_REQUESTS="256Mi"
    BACKEND_POD_MEM_LIMITS="1324Mi"
    PROXY_POD_CPU_REQUESTS="50m"
    PROXY_POD_CPU_LIMITS="100m"
    PROXY_POD_MEM_REQUESTS="16Mi"
    PROXY_POD_MEM_LIMITS="32Mi"

    docker push "akvo/flow-api-proxy:$CI_COMMIT"
    docker push "akvo/flow-api-auth0-proxy:$CI_COMMIT"
    docker push "akvo/flow-api-backend:$CI_COMMIT"
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
  -e "s/\${CI_COMMIT}/${CI_COMMIT}/" \
  ci/k8s/deployment.yaml.template > ci/k8s/deployment.yml

kubectl apply -f ci/k8s/deployment.yml
kubectl apply -f ci/k8s/service.yml
kubectl apply -f "ci/k8s/${CONFIG_MAP}/config-map.yml"
