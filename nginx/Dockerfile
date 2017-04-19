FROM openresty/openresty:1.11.2.2-alpine-fat

RUN apk add --no-cache build-base openssl-dev git

RUN /usr/local/openresty/luajit/bin/luarocks install lua-resty-openidc 1.3.1-1

EXPOSE 8081

ADD nginx.conf /usr/local/openresty/nginx/conf/nginx.conf

ENTRYPOINT ["/usr/local/openresty/bin/openresty", "-c", "/usr/local/openresty/nginx/conf/nginx.conf", "-g", "daemon off;"]
