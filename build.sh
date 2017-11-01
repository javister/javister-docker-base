#!/bin/bash -e

function build() {
    local release
    release="dry"

    while getopts ":rh" opt; do
        case $opt in
            r)
                release="release"
                ;;
            h)
                cat <<EOF
usage: build [OPTION]... [-- [docker build opts]]
  -h        show this help.
  -r        pull base image and push resulting image too.
EOF
                return 0;
                ;;
            :)
                echo "$0: option requires an argument -- '$OPTARG'" 1>&2
                return 1
                ;;
            *)
                echo "$0: invalid option -- '$OPTARG'" 1>&2
                return 1
                ;;
        esac
    done
    shift $((OPTIND-1))

    VERSION=1.0
    DATE=$(date +"%Y-%m-%d")

    IMAGE_TAG="javister-docker-docker.bintray.io/javister/javister-docker-base"
    PROXY_ARGS="--build-arg http_proxy=${http_proxy} \
                --build-arg no_proxy=${no_proxy}"
    [ "${release}" == "release" ] && docker pull centos:7

    docker build \
        --tag ${IMAGE_TAG}:latest \
        --tag ${IMAGE_TAG}:${VERSION} \
        --tag ${IMAGE_TAG}:${VERSION}-${DATE} \
        ${PROXY_ARGS} \
        $@ \
        .

    [ "${release}" == "release" ] && docker push ${IMAGE_TAG}:latest
    [ "${release}" == "release" ] && docker push ${IMAGE_TAG}:${VERSION}
    [ "${release}" == "release" ] && docker push ${IMAGE_TAG}:${VERSION}-${DATE}
}

trap "exit 1" INT TERM QUIT

build "$@"
