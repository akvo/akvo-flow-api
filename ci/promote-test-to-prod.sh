#!/usr/bin/env bash

set -u

function log {
   echo "$(date +"%T") - INFO - $*"
}

PREVIOUS_CONTEXT=$(kubectl config current-context)

function switch_back () {
    log "Switching k8s context back to ${PREVIOUS_CONTEXT}"
    kubectl config use-context "${PREVIOUS_CONTEXT}"
}

function read_version () {
    CLUSTER=$1
    log "Reading ${CLUSTER} version"
    log "running: gcloud container clusters get-credentials ${CLUSTER} --zone europe-west1-d --project akvo-lumen"
    if ! gcloud container clusters get-credentials "${CLUSTER}" --zone europe-west1-d --project akvo-lumen; then
        log "Could not change context to ${CLUSTER}. Nothing done."
        switch_back
        exit 3
    fi

    VERSION=$(kubectl get deployments flow-api -o jsonpath="{@.spec.template.metadata.labels['akvo-flow-api-version']}")
}

read_version "test"
TEST_VERSION=$VERSION

read_version "production"
PROD_VERSION=$VERSION

log "Deployed test version is $TEST_VERSION"
log "Deployed prod version is $PROD_VERSION"
log "See https://github.com/akvo/akvo-flow-api/compare/$PROD_VERSION..$TEST_VERSION"

TAG_NAME="promote-$(date +"%Y%m%d-%H%M%S")"

log "To deploy, run: "
echo "----------------------------------------------"
echo "git tag $TAG_NAME $TEST_VERSION"
echo "git push origin $TAG_NAME"
echo "----------------------------------------------"

switch_back