#!/bin/bash

source /usr/local/bin/yum-proxy

# Если переменная установлена в пустое значение - то значит настраивать прокси не надо и выходим
[ -z "$PROXY" ] && exit 0

echo -n "$PROXY_USER" > /etc/container_environment/PROXY_USER
echo -n "$PROXY_PASS" > /etc/container_environment/PROXY_PASS
echo -n "$PROXY_HOST" > /etc/container_environment/PROXY_HOST
echo -n "$PROXY_PORT" > /etc/container_environment/PROXY_PORT
echo -n "$NO_PROXY" > /etc/container_environment/NO_PROXY
echo -n "$NO_PROXY" > /etc/container_environment/no_proxy

echo -n "$http_proxy" > /etc/container_environment/http_proxy
echo -n "$https_proxy" > /etc/container_environment/https_proxy

http_proxy=$http_proxy /usr/local/bin/yum-proxy




if [ "$PROXY" ]; then
    if [ "$PROXY_USER" ]; then 
        hpy="http://$PROXY_USER:$PROXY_PASS@$PROXY_HOST:$PROXY_PORT"
    else 
        hpy="http://$PROXY_HOST:$PROXY_PORT"
    fi
fi

[ "$hpy" ] && echo "
-------------------------------------
Proxy setted up: $hpy
-------------------------------------
"

