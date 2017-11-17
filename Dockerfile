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
    BASE_RPMLIST="python34 syslog-ng cronie inotify-tools zip unzip wget less psmisc" \
    LOG_LEVEL="INFO"

RUN echo '*** Set permissions for the support tools' && \
    sync && \
    chmod --recursive +x /etc/my_init.d/*.sh /etc/service /usr/local/bin/* && \
    localedef -c -i ru_RU -f UTF-8 ru_RU.UTF-8 && \
    echo '*** Setup proxy and yum' && \
    . /usr/local/bin/yum-proxy && \
    update-ca-trust && \
    echo '*** Update all rpm packages' && \
    yum -y update && \
    echo '*** Install additional softvare' && \
    yum -y install epel-release && \
    yum -y install $BASE_RPMLIST && \
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
    https_proxy=$https_proxy curl -s "https://packagecloud.io/install/repositories/imeyer/runit/config_file.repo?os=el&dist=7&name=javister-docker-base" > /etc/yum.repos.d/imeyer_runit.repo && \
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
    yum-clean

CMD ["/usr/local/bin/my_init"]
