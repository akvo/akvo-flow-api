FROM clojure:lein-2.7.1-alpine

WORKDIR /app

ADD . /app/

RUN lein uberjar

CMD ["java","-jar","/app/target/stub-server.jar"]
