FROM openjdk:8-jdk-alpine

RUN apk add --no-cache wget unzip git

RUN mkdir /opt

ADD .akvo-flow-cache/*.zip /opt/

RUN cd /opt && \
    unzip apache-ant-1.10.1-bin.zip && \
    unzip apache-maven-3.0.5-bin.zip && \
    unzip appengine-java-sdk-1.9.50.zip

ENV ANT_HOME=/opt/apache-ant-1.10.1
ENV MAVEN_HOME=/opt/apache-maven-3.0.5

ADD flow-build.sh /opt/flow-build.sh
ADD settings.xml /opt/apache-maven-3.0.5/conf/settings.xml

CMD ["/opt/flow-build.sh"]
