FROM alpine:3.7

ARG CLOUD_SDK_VERSION=202.0.0
ARG CLOUD_SDK_SHA256=3f7d92df514aa289ba372f3601c7ebb1d9859f2d19897ddcc8e9e17fcac05b12
ENV PATH="/google-cloud-sdk/bin:${PATH}"
ENV CLOUDSDK_PYTHON_SITEPACKAGES=1

RUN set -ex ; \
    apk add --no-cache \
    bash~=4 \
    curl~=7 \
    git~=2 \
    openjdk8~=8 \
    openssh-client~=7 \
    python2~=2 \
    nss~=3 \
    py-crcmod~=1 \
    py-openssl~=17 \
    maven~=3 \
    libc6-compat~=1 \
    runit~=2 \
    shadow~=4 && \
    curl -O "https://dl.google.com/dl/cloudsdk/channels/rapid/downloads/google-cloud-sdk-${CLOUD_SDK_VERSION}-linux-x86_64.tar.gz" && \
    echo "${CLOUD_SDK_SHA256}  google-cloud-sdk-${CLOUD_SDK_VERSION}-linux-x86_64.tar.gz" > "google-cloud-sdk-${CLOUD_SDK_VERSION}-linux-x86_64.tar.gz.sha256" && \
    sha256sum -c "google-cloud-sdk-${CLOUD_SDK_VERSION}-linux-x86_64.tar.gz.sha256" && \
    tar xzf "google-cloud-sdk-${CLOUD_SDK_VERSION}-linux-x86_64.tar.gz" && \
    rm "google-cloud-sdk-${CLOUD_SDK_VERSION}-linux-x86_64.tar.gz" && \
    ln -s /lib /lib64 && \
    gcloud config set core/disable_usage_reporting true && \
    gcloud config set component_manager/disable_update_check true && \
    gcloud components install app-engine-java && \
    rm -rf /google-cloud-sdk/.install/.backup && \
    rm -rf /google-cloud-sdk/.install/.download && \
    adduser -D -h /home/akvo -s /bin/bash akvo akvo

WORKDIR /app

CMD [ "./run-as-user.sh", "./start.sh" ]
