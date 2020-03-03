#!/bin/bash -e

function build() {
    local release="verify"
    local downstream="no"

    while getopts ":rhd" opt; do
        case $opt in
            r)
                release="deploy"
                ;;
            d)
                downstream="yes"
                ;;
            h)
                cat <<EOF
usage: build [OPTION]... [-- [docker build opts]]
  -h        show this help.
  -c        use cache from previous build
  -r        push resulting image.
  -p        don't pull base image.
  -d        trigger downstream builds on Travis CI
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

    mvn -B ${release}

    if [ "${downstream}" == "yes" ]; then
        while read -u 10 repo; do
            echo "*** Trigger downstream build ${repo}"
            URL=$(echo "${repo}" | sed -r "s/([0-9a-zA-Z_-]+).([0-9a-zA-Z_-]+)/\\1%2F\\2/g")

            body='{
            "request": {
            "branch":"master"
            }}'

            curl -s -X POST \
               -H "Content-Type: application/json" \
               -H "Accept: application/json" \
               -H "Travis-API-Version: 3" \
               -H "Authorization: token ${TRAVIS_TOKEN}" \
               -d "$body" \
               https://api.travis-ci.org/repo/${URL}/requests
        done 10<downstream.txt
    fi
}

CURRENT_DIR=$(pwd)

if [ -d ${CURRENT_DIR}/tmp ]; then
    rm -rf ${CURRENT_DIR}/tmp
fi

trap "exit 1" INT TERM QUIT

build "$@"
