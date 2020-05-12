#!/bin/bash -e

PUID=${PUID:-911}
PGID=${PGID:-911}
PUSER=${PUSER:-system}
LOG_LEVEL=${LOG_LEVEL:-INFO}

verbose=""
if [[ "${LOG_LEVEL}" == "DEBUG" ]]; then verbose="--changes"; fi

[[ $(id -u system > /dev/null 2>&1; echo $?) ]] && CURR_USER="system" || CURR_USER="${PUSER}"
mdebug "Current user: ${CURR_USER}"

chown ${verbose} ${CURR_USER} /var/spool/mail/${CURR_USER}

if [[ ! "$(id -u ${CURR_USER})" -eq "${PUID}" ]]; then usermod -o -u "${PUID}" ${CURR_USER} ; fi
if [[ ! "$(id -g ${CURR_USER})" -eq "${PGID}" ]]; then groupmod -o -g "${PGID}" ${CURR_USER} ; fi

mdebug "
-------------------------------------
GID/UID for ${CURR_USER}
-------------------------------------
User uid:    $(id -u ${CURR_USER})
User gid:    $(id -g ${CURR_USER})
-------------------------------------
"
chown ${verbose} ${CURR_USER}:${CURR_USER} /app /config /defaults

if [[  "$(id -nu ${PUID})" != "${PUSER}" ]]; then
    mdebug "Change user name from [$(id -nu ${PUID})] to [${PUSER}]";
    usermod -l ${PUSER} $(id -nu ${PUID});
fi

if [[  "$(id -ng ${PUID})" != "${PUSER}" ]]; then
    mdebug "Change user group from [$(id -ng ${PUID})] to [${PUSER}]";
    groupmod --new-name ${PUSER} $(id -ng ${PUID});
fi
