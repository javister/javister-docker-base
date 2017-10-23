FROM centos:7
MAINTAINER Viktor Verbitsky <vektory79@gmail.com>

COPY files /

ENV PUID=911 \
    PGID=911 \
    TERM="xterm" \
    TZ="Europe/Moscow" \
    KILL_PROCESS_TIMEOUT=5 \
    KILL_ALL_PROCESSES_TIMEOUT=5 \
    RPMLIST="" \
    BUILD_RPMLIST="" \
    BASE_RPMLIST="python34 syslog-ng cronie inotify-tools wget less psmisc"

RUN source /usr/local/sbin/proxyenv && \
    localedef -c -i ru_RU -f UTF-8 ru_RU.UTF-8 && \
    echo '*** Setup proxy and yum' && \
    /usr/local/sbin/yum-proxy && \
    update-ca-trust && \
    echo '*** Update all rpm packages' && \
    yum -y update && \
    echo '*** Install additional softvare' && \
    yum -y install epel-release && \
    yum -y install $BASE_RPMLIST && \
    echo '*** Set permissions for the support tools' && \
    chmod 755 /usr/local/sbin/* && \
    chmod 755 /usr/local/bin/* && \
    sync && \
    echo '*** Add user "system"' && \
    useradd -u $PUID -U -d /config -s /bin/false system && \
    usermod -G users system && \
    echo '*** Подготовка каталогов конфигураций' && \
    mkdir -p /app /config /defaults && \
    chown system:system /app /config /defaults && \
    echo '*** Install init process.' && \
    mkdir -p /etc/my_init.d && \
    mkdir -p /etc/container_environment && \
    touch /etc/container_environment.sh && \
    touch /etc/container_environment.json && \
    chmod 700 /etc/container_environment /etc/container_environment.sh /etc/container_environment.json && \
    ln -s /etc/container_environment.sh /etc/profile.d/ && \
    echo '*** Install runit (http://smarden.org/runit/).' && \
    (curl -s "https://packagecloud.io/install/repositories/imeyer/runit/config_file.repo?os=el&dist=7&name=javister-docker-base" > /etc/yum.repos.d/imeyer_runit.repo || true) && \
    yum install -y runit && \
    rm -f /tmp/* && \
    echo '*** Configure a syslog daemon and logrotate.' && \
    touch /var/log/syslog && \
    mknod -m 640 /dev/xconsole p && \
    chmod u=rw,g=r,o= /var/log/syslog && \
    echo '*** Configure cron daemon.' && \
    chmod 600 /etc/crontab && \
    sed -i 's/^\s*session\s\+required\s\+pam_loginuid.so/# &/' /etc/pam.d/crond && \
    echo '*** Clean up yum caches' && \
    yum-clean && \
    chmod +x /etc/my_init.d/*.sh

CMD ["/usr/local/bin/my_init"]
