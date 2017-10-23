#!/bin/bash

PUID=${PUID:-911}
PGID=${PGID:-911}

if [ ! "$(id -u system)" -eq "$PUID" ]; then usermod -o -u "$PUID" system ; fi
if [ ! "$(id -g system)" -eq "$PGID" ]; then groupmod -o -g "$PGID" system ; fi

echo "
-------------------------------------
GID/UID
-------------------------------------
User uid:    $(id -u system)
User gid:    $(id -g system)
-------------------------------------
"
chown system:system /app
chown system:system /config
chown system:system /defaults