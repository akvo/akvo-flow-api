FROM openresty/openresty:1.15.8.2-7-alpine-fat-nosse42

RUN apk add --no-cache \
    build-base~=0.5 \
    openssl-dev~=1 \
    git~=2 && \
    luarocks install lua-resty-openidc 1.7.2-1 && \
    cp /etc/ssl/certs/ca-certificates.crt /etc/ssl/certs/ca-certificates.pem && \
    mkdir -p /data/nginx/cache

EXPOSE 8082

COPY nginx.conf /usr/local/openresty/nginx/conf/nginx.conf

ENTRYPOINT ["/usr/local/openresty/bin/openresty", "-c", "/usr/local/openresty/nginx/conf/nginx.conf", "-g", "daemon off;"]
