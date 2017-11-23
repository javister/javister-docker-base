#!/usr/bin/env bash

mdebug "Setup locale and language"
mdebug "LANG=${LANG}"

if [ "${LANG}" ]; then
    LANGUAGE=$(echo "$LANG" | sed -e $'s/\(..\)_\(..\)\\.\(.*\)/\\1/g')
    COUNTRY=$(echo "$LANG" | sed -e $'s/\(..\)_\(..\)\\.\(.*\)/\\2/g')
    LOCALE=$(echo "$LANG" | sed -e $'s/\(..\)_\(..\)\\.\(.*\)/\\3/g')

    echo -n "${LANGUAGE}" > /etc/container_environment/LANGUAGE
    echo -n "${COUNTRY}" > /etc/container_environment/COUNTRY
    echo -n "${LOCALE}" > /etc/container_environment/LOCALE

    mdebug "LANGUAGE=${LANGUAGE}"
    mdebug "COUNTRY=${COUNTRY}"
    mdebug "LOCALE=${LOCALE}"

    localedef -c -i ${LANGUAGE}_${COUNTRY} -f ${LOCALE} "$LANG"
fi