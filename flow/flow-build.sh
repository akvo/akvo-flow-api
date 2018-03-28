#!/usr/bin/env sh

set -eu

cd /opt/akvo-flow

git checkout develop
git pull
git fetch --tags

TAG=v1.9.29

git checkout $TAG
git reset --hard

cd GAE
cp build.properties.template build.properties
sed -i 's/^sdk\.dir=.*/sdk\.dir=\/opt\/appengine-java-sdk-1.9.50/g' build.properties

"${ANT_HOME}/bin/ant" compile datanucleusenhance

cd war/WEB-INF/classes

jar cf /tmp/akvo-flow.jar \
    META-INF/jdoconfig.xml \
    $(find . \
	   -name '*.class' \
	   -and -path '*domain*' \
	   -or -path '*dao*' \
	   -or -path '*framework*' \
	   -or -path '*common*')

"${MAVEN_HOME}/bin/mvn" install:install-file \
			-Dfile=/tmp/akvo-flow.jar \
			-DartifactId=data-access \
			-Dversion="${TAG}" \
			-DgroupId=org.akvo.flow \
			-Dpackaging=jar
