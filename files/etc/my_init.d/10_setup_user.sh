#!/bin/bash

PUID=${PUID:-911}
PGID=${PGID:-911}

if [ ! "$(id -u system)" -eq "$PUID" ]; then usermod -o -u "$PUID" system ; fi
if [ ! "$(id -g system)" -eq "$PGID" ]; then groupmod -o -g "$PGID" system ; fi

mdebug "
-------------------------------------
GID/UID
-------------------------------------
User uid:    $(id -u system)
User gid:    $(id -g system)
-------------------------------------
"

verbose=""
if [ "${LOG_LEVEL}" == "TRACE" ]; then verbose="--changes"; fi

chown ${verbose} system:system /app
chown ${verbose} system:system /config
chown ${verbose} system:system /defaults