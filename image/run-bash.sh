#!/bin/bash
PROXY_ARGS="--env http_proxy=${http_proxy} \
            --env no_proxy=${no_proxy}"
docker run \
    --rm \
    -it \
    -e PUID=$(id -u) \
    -e PGID=$(id -g) \
    ${PROXY_ARGS} \
    javister-docker-docker.bintray.io/javister/javister-docker-base \
    my_init --skip-runit -- bash $@
