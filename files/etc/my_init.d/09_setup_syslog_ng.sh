#!/usr/bin/env bash
set -e

export SYSLOGNG_FILTER=" and not filter(f_debug)"
export SYSLOGNG_CRON_FILTER=" and not filter(f_debug)"

case ${LOG_LEVEL} in
    DEBUG)
        export SYSLOGNG_FILTER=""
        export SYSLOGNG_CRON_FILTER=""
        ;;
    INFO)
        export SYSLOGNG_FILTER=" and not filter(f_debug) and not program(CROND)"
        export SYSLOGNG_CRON_FILTER=" and not filter(f_debug) and not filter(f_notice) and not program(CROND)"
        sed --in-place "s/cron.hourly/cron.hourly > \\/dev\\/null/g" /etc/cron.d/0hourly
        ;;
    WARNING)
        export SYSLOGNG_FILTER=" and not filter(f_debug) and not filter(f_info) and not filter(f_notice) and not program(CROND)"
        export SYSLOGNG_CRON_FILTER=" and not filter(f_debug) and not filter(f_info) and not filter(f_notice) and not program(CROND)"
        sed --in-place "s/cron.hourly/cron.hourly > \\/dev\\/null/g" /etc/cron.d/0hourly
        ;;
    ERROR)
        export SYSLOGNG_FILTER=" and not filter(f_debug) and not filter(f_info) and not filter(f_notice) and not filter(f_warn) and not filter(f_messages) and not program(CROND)"
        export SYSLOGNG_CRON_FILTER=" and not filter(f_debug) and not filter(f_info) and not filter(f_notice) and not filter(f_warn) and not filter(f_messages) and not program(CROND)"
        sed --in-place "s/cron.hourly/cron.hourly > \\/dev\\/null/g" /etc/cron.d/0hourly
        ;;
    CRITICAL)
        export SYSLOGNG_FILTER=" and not filter(f_debug) and not filter(f_info) and not filter(f_notice) and not filter(f_warn) and not filter(f_messages) and not filter(f_err) and not program(CROND)"
        export SYSLOGNG_CRON_FILTER=" and not filter(f_debug) and not filter(f_info) and not filter(f_notice) and not filter(f_warn) and not filter(f_messages) and not filter(f_err) and not program(CROND)"
        sed --in-place "s/cron.hourly/cron.hourly > \\/dev\\/null/g" /etc/cron.d/0hourly
        ;;
esac

expandenv /etc/syslog-ng/syslog-ng.conf.template > /etc/syslog-ng/syslog-ng.conf
