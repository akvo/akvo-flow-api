# Build gosu
FROM alpine:3.7 as gosu-builder
ENV GOSU_VERSION 1.10
RUN set -ex; \
	\
	apk add --no-cache --virtual .gosu-deps \
		dpkg \
		gnupg \
		openssl \
	; \
	\
	dpkgArch="$(dpkg --print-architecture | awk -F- '{ print $NF }')"; \
	wget -O /usr/local/bin/gosu "https://github.com/tianon/gosu/releases/download/$GOSU_VERSION/gosu-$dpkgArch"; \
	wget -O /usr/local/bin/gosu.asc "https://github.com/tianon/gosu/releases/download/$GOSU_VERSION/gosu-$dpkgArch.asc"; \
	\
# verify the signature
	export GNUPGHOME="$(mktemp -d)"; \
	gpg --keyserver eu.pool.sks-keyservers.net --recv-keys B42F6819007F00F88E364FD4036A9C25BF357DD4; \
	gpg --batch --verify /usr/local/bin/gosu.asc /usr/local/bin/gosu; \
	rm -rf "$GNUPGHOME" /usr/local/bin/gosu.asc; \
	\
	chmod +x /usr/local/bin/gosu; \
# verify that the binary works
	gosu nobody true;

# Main image
FROM alpine:3.7

ARG CLOUD_SDK_VERSION=202.0.0
ARG CLOUD_SDK_SHA256=3f7d92df514aa289ba372f3601c7ebb1d9859f2d19897ddcc8e9e17fcac05b12
ENV PATH="/google-cloud-sdk/bin:${PATH}"
ENV CLOUDSDK_PYTHON_SITEPACKAGES=1

COPY --from=gosu-builder /usr/local/bin/gosu /usr/local/bin/gosu

RUN set -ex ; \
    apk add --no-cache \
    bash=4.4.19-r1 \
    curl=7.60.0-r1 \
    git=2.15.0-r1 \
    openjdk8=8.151.12-r0 \
    openssh-client=7.5_p1-r8 \
    python2=2.7.14-r2 \
    py-crcmod=1.7-r0 \
    py-openssl=17.2.0-r0 \
    maven=3.5.2-r0 \
    libc6-compat=1.1.18-r3 \
    shadow=4.5-r0 && \
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
