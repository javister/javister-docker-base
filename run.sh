#!/bin/bash
PROXY_ARGS="--env http_proxy=${http_proxy} \
            --env no_proxy=${no_proxy}"
docker run --rm -it ${PROXY_ARGS} javister-docker-docker.bintray.io/javister/javister-docker-base
