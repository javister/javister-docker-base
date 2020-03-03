#!/bin/bash

PUID=${PUID:-911}
PGID=${PGID:-911}

verbose=""
if [ "${LOG_LEVEL}" == "DEBUG" ]; then verbose="--changes"; fi

chown ${verbose} system /var/spool/mail/system

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

chown ${verbose} system:system /app /config /defaults
