FROM openresty/openresty:1.13.6.2-2-alpine-fat

RUN apk add --no-cache build-base openssl-dev git && mkdir -p /data/nginx/cache

RUN /usr/local/openresty/luajit/bin/luarocks install lua-resty-openidc 1.7.2-1
RUN /usr/local/openresty/luajit/bin/luarocks install lua-resty-jwt 0.2.0-0

EXPOSE 8082

ENTRYPOINT ["/usr/local/openresty/bin/openresty", "-c", "/usr/local/openresty/nginx/conf/nginx.conf", "-g", "daemon off;"]

RUN cp /etc/ssl/certs/ca-certificates.crt /etc/ssl/certs/ca-certificates.pem

ADD nginx.conf /usr/local/openresty/nginx/conf/nginx.conf
