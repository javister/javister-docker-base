#!/bin/bash -e

IMAGE_NAME=$(xmlstarlet sel -N p=http://maven.apache.org/POM/4.0.0 -t -v "/p:project/p:properties/p:docker.image" pom.xml)
IMAGE_VERSION=$(xmlstarlet sel -N p=http://maven.apache.org/POM/4.0.0 -t -v "/p:project/p:properties/p:revision" pom.xml)
if docker pull ${IMAGE_NAME}:${IMAGE_VERSION}; then
    [[ "$(docker run --rm ${IMAGE_NAME}:${IMAGE_VERSION} yum check-update -q | wc --lines)" -gt "2" ]] && mvn -B -DforcePush verify
else
    mvn -B -DforcePush verify
fi
