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
        export SYSLOGNG_FILTER=" and not filter(f_debug)"
        export SYSLOGNG_CRON_FILTER=" and not filter(f_debug) and not filter(f_notice)"
        ;;
    WARNING)
        export SYSLOGNG_FILTER=" and not filter(f_debug) and not filter(f_info) and not filter(f_notice)"
        export SYSLOGNG_CRON_FILTER=" and not filter(f_debug) and not filter(f_info) and not filter(f_notice)"
        ;;
    ERROR)
        export SYSLOGNG_FILTER=" and not filter(f_debug) and not filter(f_info) and not filter(f_notice) and not filter(f_warn) and not filter(f_messages)"
        export SYSLOGNG_CRON_FILTER=" and not filter(f_debug) and not filter(f_info) and not filter(f_notice) and not filter(f_warn) and not filter(f_messages)"
        ;;
    CRITICAL)
        export SYSLOGNG_FILTER=" and not filter(f_debug) and not filter(f_info) and not filter(f_notice) and not filter(f_warn) and not filter(f_messages) and not filter(f_err)"
        export SYSLOGNG_CRON_FILTER=" and not filter(f_debug) and not filter(f_info) and not filter(f_notice) and not filter(f_warn) and not filter(f_messages) and not filter(f_err)"
        ;;
esac

expandenv /etc/syslog-ng/syslog-ng.conf.template > /etc/syslog-ng/syslog-ng.conf
