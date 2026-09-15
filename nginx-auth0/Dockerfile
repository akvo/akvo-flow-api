FROM openresty/openresty:1.15.8.2-7-alpine-fat-nosse42

# luarocks.org's root manifest has outgrown Lua 5.1's 65536-constant limit, so
# this luarocks can no longer parse it ("main function has more than 65536
# constants") and finds nothing. Install each rock from its publisher's own
# manifest instead, which stays small enough to load. Versions are pinned to
# what 1.7.6-3 resolves against -- its rockspec caps lua-resty-session at 3.10,
# because 4.x changed the API it expects.
ARG M=https://luarocks.org/manifests

RUN apk add --no-cache \
    build-base~=0.5 \
    openssl-dev~=1 \
    git~=2 && \
    luarocks install --server=$M/pintsized     lua-resty-http    0.18.0-0 && \
    luarocks install --server=$M/bungle        lua-resty-session 3.10-1 && \
    luarocks install --server=$M/fffonion      lua-resty-openssl 1.9.0-1 && \
    luarocks install --server=$M/cdbattags     lua-resty-jwt     0.2.4-1 && \
    luarocks install --server=$M/hanszandbelt  lua-resty-openidc 1.7.6-3 && \
    cp /etc/ssl/certs/ca-certificates.crt /etc/ssl/certs/ca-certificates.pem && \
    mkdir -p /data/nginx/cache

EXPOSE 8082

COPY nginx.conf /usr/local/openresty/nginx/conf/nginx.conf
COPY authn.lua /usr/local/openresty/nginx/conf/authn.lua

ENTRYPOINT ["/usr/local/openresty/bin/openresty", "-c", "/usr/local/openresty/nginx/conf/nginx.conf", "-g", "daemon off;"]
