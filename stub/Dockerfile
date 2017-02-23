FROM mhart/alpine-node:7

RUN npm install -g stubby

EXPOSE 8882

WORKDIR /opt/stub

ADD *.json ./

CMD ["stubby","--data","config.json"]
