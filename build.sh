#!/bin/bash -e

VERSION=1.0
DATE=$(date +"%Y-%m-%d")

IMAGE_TAG="javister-docker-docker.bintray.io/javister/javister-docker-base"
PROXY_ARGS="--build-arg http_proxy=${http_proxy} \
            --build-arg no_proxy=${no_proxy}"
docker pull centos:7
docker build --tag ${IMAGE_TAG} --tag ${IMAGE_TAG}:${VERSION} --tag ${IMAGE_TAG}:${VERSION}-${DATE} ${PROXY_ARGS} .
docker push ${IMAGE_TAG}:${VERSION}
docker push ${IMAGE_TAG}:${VERSION}-${DATE}
