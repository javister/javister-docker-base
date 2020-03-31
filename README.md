# Базовый образ CentOS, адаптированный для комфортной работы в Docker

[ ![Download](https://api.bintray.com/packages/javister/docker/javister%3Ajavister-docker-base/images/download.svg) ](https://bintray.com/javister/docker/javister%3Ajavister-docker-base/_latestVersion)
[ ![Download](https://api.bintray.com/packages/javister/dockertesting/javister-docker-base/images/download.svg) ](https://bintray.com/javister/dockertesting/javister-docker-base/_latestVersion)
![Build master branch](https://github.com/javister/javister-docker-base/workflows/Build%20master%20branch/badge.svg)
![Check updates](https://github.com/javister/javister-docker-base/workflows/Check%20updates/badge.svg)

TODO: дополнительно описать вспомогательные скрипты для уменшения бойлерплейта

TODO: дополнительно описать про работу с прокси

_Идеи почерпнуты из образа [phusion/baseimage-docker](https://github.com/phusion/baseimage-docker)_

_Пустой образ javister-docker-base потребляет менее 17 Мб памяти, но при этом является гораздо более мощным, чем стандартные 
образы. Смотри ниже почему._

javister-docker-base это специальный образ [Docker](https://www.docker.com) который настроен для корректной работы в рамках
контейнера Docker. По сути это образ CentOS плюс:

 * Модификации для большей совместимости с Docker.
 * Инструменты администрирования, которые особенно полезны в контексте Docker.
 * Механизмы для упрощения запуска множественных процессов [без нарушения философии Docker](#docker_single_process).

Этот образ предназначен для использования в качестве базового для других образов Docker.

javister-docker-base доступен для загрузки по имени `javister-docker-docker.bintray.io/javister/javister-docker-base`!

### В чем проблема с оригинальным образом CentOS?

CentOS изначально не проектировалась для запуска внутри Docker. Её система инициализации, systemd, предполагает запуск на физическом 
железе или виртуальной машине, но не внутри контейнера Docker. В тоже время, внутри контейнера нет необходимости в полной системе. 
В этом случае необходима минимальная система только для того, чтобы запустить приложение. Конфигурирование такой минимальной системы
в рамках контейнера имеет много странных краевых случаев, которые сложно учесть, если вы не знакомы с системной моделью Unix сверху до низу.
И это постоянно приводит к большому количеству странных проблем.

javister-docker-base старается привести всё к правильному виду. Дальнейшее изложение описывает всё, что было модифицировано.

<a name="why_use"></a>
### Зачем использовать javister-docker-base?

Вы можете сконфигурировать оригинальный образ `centos` самостоятельно через собственный Dockerfile, так зачем же использовать javister-docker-base?

 * Настройка базовой системы для корректной работы в Docker - не простая задача. Как упоминалось ранее, тут много подводных камней.
   В то время, как вы сможете сделать всё правильно, вы просто переизобретёте javister-docker-base. Так что javister-docker-base просто сэкономит вам время.
 * Он экономит время при написании корректного Dockerfile. Вы можете не беспокоиться о базовой системе и просто сфокусировать на основной задаче.
 * Он экономит время при запуске `docker build`, позволяя вам быстрее разрабатывать Dockerfile.
 * Он экономит время загрузки при передеплое. Docker нужно загрузить базовый образ только один раз. Во время первого деплоя.
   При каждом последующем деплое будут загружены только изменения, сделанные повер базового образа.

-----------------------------------------

**Содержание**

 * [Что внутри образа?](#whats_inside)
   * [Обзор](#whats_inside_overview)
   * [Подождите! Я думал, что в Docker должен быть один процесс на контейнер?](#docker_single_process)
   * [И что же? javister-docker-base защищает идею "толстых контейнеров" или "использование контейнеров как виртуальны машин"?](#fat_containers)
 * [Изучаем javister-docker-base](#inspecting)
 * [Используем javister-docker-base как базовый образ](#using)
   * [Начальные азы](#getting_started)
   * [Создаём дополнительные сервисы](#adding_additional_daemons)
   * [Запускаем скрипты при старте контейнера](#running_startup_scripts)
     * [Остановка ваших процессов](#environment_variables)
   * [Переменные окружения](#environment_variables)
     * [Централизованное объявление переменных окружения](#envvar_central_definition)
     * [Дамп переменных окружения](#envvar_dumps)
     * [Изменение переменных окружения](#modifying_envvars)
     * [Безопасность](#envvar_security)
     * [Доступные переменные окружения](#available_envvars)
 * [Администрирование контейнера](#container_administration)
   * [Запуск одноразовых команд в контейнере](#oneshot)
   * [Запуск команд в существующем, запущенном контейнере](#run_inside_existing_container)
   * [Вход в контейнер, или запуск соманды внутри него, с помощью `docker exec`](#login_docker_exec)
     * [Примеры использования](#docker_exec)
 * [Сборка](#build)

-----------------------------------------

<a name="whats_inside"></a>
## Что внутри образа?

<a name="whats_inside_overview"></a>
### Обзор

| Component        | Why is it included? / Remarks |
| ---------------- | ------------------- |
| CentOS 7 | Базовая система. |
| **Корректный** стартовый процесс | Основная статья: [Docker and the PID 1 zombie reaping problem](http://blog.phusion.nl/2015/01/20/docker-and-the-pid-1-zombie-reaping-problem/). <br><br>В соответствии с моделью процессов Unix, [стартовый процесс](https://en.wikipedia.org/wiki/Init) -- PID 1 -- наследует все [потерянные дочерние прочессы](https://en.wikipedia.org/wiki/Orphan_process) и должен [уничтожать их](https://en.wikipedia.org/wiki/Wait_(system_call)). Большинство контейнеров Docker не имеют стартового процесса, который бы делал это корректно. В результате контейнеры со временем наполняются [процессами-зомби](https://en.wikipedia.org/wiki/Zombie_process). <br><br>Более того, `docker stop` отправляет SIGTERM в стартовый процесс, что останавливает все сервисы. К сожалению большинство систем инициализации не обрабатывает это корректно в рамках Docker т.к. они разработаны для физической остановки сервера, а не остановки контейнера. Это приводит к тому, что процессы просто убиваются сигналом SIGKILL, что не оставляет им шансов на корректную остановку. Это может приводить к повреждениям файлов. <br><br>javister-docker-base поставляется со стартовым процессом `/sbin/my_init` который обрабатывает такие ситуации корректно. |
| syslog-ng | Демон syslog, необходимы для того, чтобы все процессы (включая базовые) могли логгироваться в /var/log/syslog. Если никакого демона syslog не запущено, то множество важных сообщений молча проглатываются. <br><br>Демон работает только локально, в контейнере. Все сообщения syslog перенаправляются в `docker logs`. |
| logrotate | Регулярное ротирование и сжатие логов. |
| cron | Демон cron должен необходим для запуска регулярных задач. |
| [runit](http://smarden.org/runit/) | Замена systemd из CentOS. Используется для управления сервисами. Его гораздо легче использовать и и гораздо более легковесный, чем systemd. Он поддерживает перезапуск сервисов в случае их падения.
| `setuser` | Инструмент для запуска процессов от имени другого пользователя. Проще использовать, чем `su`. Имеет меньше векторов атак, чем `sudo`. И, в отличии от `chpst`, этот этот инструмент устанавливает `$HOME` правильно. Доступен как `/sbin/setuser`. |

<a name="docker_single_process"></a>
### Подождите! Я думал, что в Docker должен быть один процесс на контейнер?

Разработчики Docker продвигают философию одного *логического сервиса* на контейнер. В тоже время логический сервис вполне может
состоять из нескольких процессов ОС.

Всё что предлагает javister-docker-base - просто запуск нескольких процессов внутри одного контейнера. Это необходимо как минимум
для решения [проблемы с PID 1](http://blog.phusion.nl/2015/01/20/docker-and-the-pid-1-zombie-reaping-problem/) 
и проблемы "чёрной дыры syslog". Т.о. запуск нескольких процессов решается вполне реальная проблема уровня Unix OS
при минимальных накладных расходах и без необходимости разбиения единого контейнера на несколько логических сервисов.

В тоже время, разбиение одного логического сервиса на несколько потоков OS может быть важным для обеспечения безопасности. Путём запуска
процессов от имени разных пользователей, можно ограничить влияние разных уязвимостей. javister-docker-base предоставляет инструмент для переключения пользователя
запускаемого процесса: `setuser`.

Но допустимо ли использование нескольких *логических сервисов* в одном контейнере? Не обязательно, но и не возбраняется.
В то время, как разработчики Docker очень упрямы и имеют очень жёсткую философию о том как *должен* быть построен контейнер,
javister-docker-base не создаёт каких-либо ограничений на этот счёт. Более того: в разных ситуациях может быть выгодно поступать по разному.
При выборе декомпозиции сервисов нужно отталкиваться от конкретной задачи и требований. Где-то разуменее делать по контейнеру
на каждый логический сервис, а где-то наоборот, выгоднее объединить всю задачу в одном контейнере.

<a name="fat_containers"></a>
### И что же? javister-docker-base защищает идею "толстых контейнеров" или "использование контейнеров как виртуальны машин"?

Кому-то может показаться, что javister-docker-base пропагандирует использование контейнероа как виртуалной машины,
потому, что javister-docker-base пропагандирует использование нескольких процессов в одном контейнере. И, как следствие,
им может показаться, что javister-docker-base не следует философии Docker. Это не так.

Разработчики Docker пропагандируют один *логический сервис* внутри одного контейнера. Но javister-docker-base в общем-то и не нарушает это.
javister-docker-base пропагандирует использование нескольких *процессов ОС* в одном контейнере. И один логический сервис может состоять 
из нескольких процессов ОС.

Как следствие javister-docker-base не отрицает философию Docker. Фактически, во многих изменениях этого образа, вводится явное следование данной философии.
Например использование переменных окружения для передачи параметров в контейнер - это именно "Docker way", как и предоставление
[механизма для простой работы с переменными окружения](#environment_variables) в случае нескольких процессов, которые могут быть запущены под разными пользователями.

<a name="inspecting"></a>
## Изучаем javister-docker-base

Чтобы поизучать образ можно запустить команду:

    docker run --rm -t -i javister-docker-docker.bintray.io/javister/javister-docker-base:<VERSION> my_init -- bash

Где `<VERSION>` - указание одной из версий javister-docker-base. Если хочется запустить самую последнюю версию, то `:<VERSION>` можно опустить.

Ничего не нужно выкачивать вручную. Команда сама загрузит образ javister-docker-base из реестра Docker.

<a name="using"></a>
## Используем javister-docker-base как базовый образ

<a name="getting_started"></a>
### Начальные азы

Полное имя образа - `javister-docker-docker.bintray.io/javister/javister-docker-base`. Он хранится в репозитарии [Bintray](https://bintray.com/javister/docker).

```
    # Используйте javister-docker-docker.bintray.io/javister/javister-docker-base в качестве базового образа. 
    # Чтобы гарантировать воспроизводимость образа используйте конкретную версию, а не последнюю!
    FROM javister-docker-docker.bintray.io/javister/javister-docker-base:<VERSION>

    # Использовать систему инициализации из javister-docker-base.
    CMD ["/sbin/my_init"]

    # ...вставте свои инструкции сборки сюда...

    # Очистите YUM после окончания сборки.
    RUN yum-clean
```

<a name="adding_additional_daemons"></a>
### Создаём дополнительные сервисы

Вы можете добавить дополнительные демоны (например ваше собственное приложение) в образ путём создания указаний для runit. Для этого нужно только написать маленький shell-скрипт,
который запустит ваш демон, и runit будет поддерживать его в рабочем состоянии, перезапуская его в случае падения.

Shell-скрипт должен называться `run`, должен быть исполняемым и размещаться в каталоге `/etc/service/<NAME>`.

Вот пример, демонстрирующи как можно оформить запуск сервера memcached в runit.

Создайте скрипт `memcached.sh` (и вызовите на нём команду `chmod +x memcached.sh`):

    #!/bin/sh
    # `/sbin/setuser memcache` запускает указанную команду под пользователем `memcache`.
    # Если вы упустите эту часть, то демон запустится под пользователем root.
    exec /sbin/setuser memcache /usr/bin/memcached >>/var/log/memcached.log 2>&1

В `Dockerfile` добавте:

    RUN mkdir /etc/service/memcached
    COPY memcached.sh /etc/service/memcached/run
    RUN chmod +x /etc/service/memcached/run

Обратите внимание, что shell-скрипт должен запускать демона в **синхронном режиме, без "демонизации" и форкания процессов**. 
Обычно демоны предоставляют специальные флаги командной строки для этого. Или опции в файлах конфигурации.

<a name="running_startup_scripts"></a>
### Запускаем скрипты при старте контейнера

Система инициализации образа javister-docker-base, `/sbin/my_init`, запускает скрипты в процессе запуска в следующем порядке::

 * Все запускаемые скрипты в каталоге `/etc/my_init.d`, если каталог существует. Скрипты запускаются в порядке алфавитной сортировки.
 * Скрипт `/etc/rc.local`, если файл существует.

Все скрипты должны корректно завершаться, т.е. иметь код возврата 0. Если любой скрипт завершается с кодом возврата, отличным от нуля, то загрузка будет прервана с ошибкой.

**Важное замечание:** если вы запускаете контейнер в интерактивном режиме (например когда вы запускаете контейнер с ключами `-it`), 
а не в режиме демона, то вы посылаете stdout прямо в консоль (`-i` interactive `-t` terminal). И если вы не
вызвали `/sbin/my_init` в параметрах запуска, то `/sbin/my_init` не будет вызван, следовательно вашискрипты тоже не будут вызваны
в процессе запуска контейнера.

Следующий пример демонстрирует создание скрипта запуска. Этот скрипт просто логирует время запуска контейнера в файл /tmp/boottime.txt.

Создайте файл `logtime.sh`:

    #!/bin/sh
    date > /tmp/boottime.txt

И добавте в `Dockerfile` следующие команды:

    RUN mkdir -p /etc/my_init.d
    COPY logtime.sh /etc/my_init.d/logtime.sh
    RUN chmod +x /etc/my_init.d/logtime.sh

<a name="environment_variables"></a>
#### Остановка ваших процессов

`/sbin/my_init` управляет остановкой дочерних процессов при остановке контейнера. Когда он получает сигнал SIGTERM
он передаёт этот сигнал в дочерние процессы для корректного их завершения. Если ваш процесс запускается с помощью
shell-скрипта, то убедитесь, что он запускается с помощью команды `exec`, в противном случае оболочка получит сигнал,
но не передаст его в запущенный ею процесс.

`/sbin/my_init` прерывает процесс (SIGKILL) после 5 секундного таймаута. Этот таймаут может быть настроен через
переменные окружения:

    # Дать дочерним процессам 5 минут на завершение
    ENV KILL_PROCESS_TIMEOUT=300
    # Дать всем остальным процессам (таким, как те, которые были форкнуты) 5 минут на завершение
    ENV KILL_ALL_PROCESSES_TIMEOUT=300

### Переменные окружения

Если вы используете `/sbin/my_init` в качестве главной команды контейнера, то все переменные окружения устанавливаемые с помощью
`docker run --env` или команды `ENV` в Dockerfile, будут переданы в `my_init`. Эти переменные
так же будут переданы во все дочерние процессы, включая стартовые скрипты из `/etc/my_init.d`, 
Runit и сервисы управляемые через Runit. Однако есть несколько предостережений, о которых следует знать:

 * Переменные окружения в Unix наследуются попроцессно. Это означает, что в общем случае
   дочерний процесс не может изменить переменные окружения всех остальных процессов.
 * В связи с предыдущим фактом, не существует нормального централизованного места для добавления переменных
   окружения для всех приложений и сервисов сразу. RHEL, Debian и похожие дистрибутивы Linux имеют файл `/etc/environment`, но он помогает
   только в некоторых ситуациях.
 * Некоторые сервисы изменяют переменные окружения для дочерних процессов. Один из них - Nginx: он удаляет
   все переменные окружения, пока ему явно не укажешь какие переменные необходимо оставить через опцию конфигурации `env`.
   Если вы хостите какое-либо приложение на Nginx, то оно не увидит переменные окружения, которые были изначально переданы через Docker.
 * javister-docker-base игнорирует переменные HOME, SHELL, USER и целый ряд других переменных окружения, потому что если _не_ 
   игнорировать их, то это нарушит работу контейнеров с несколькими пользователями. См. 
   https://github.com/phusion/baseimage-docker/pull/86 -- В качестве обходного пути для установки переменной окружения `HOME` 
   можно использовать команду: `RUN echo /root > /etc/container_environment/HOME`. 
   См. https://github.com/phusion/baseimage-docker/issues/119

`my_init` предоставляет решение всех этих проблем.

<a name="envvar_central_definition"></a>
#### Централизованное объявление переменных окружения

В ходе запуска, перед запуском каких-либо [стартовых скриптов](#running_startup_scripts), `my_init` импортирует переменные окружения
из каталога `/etc/container_environment`. Этот каталог содержит файлы поименованные в соответствии с именами переменных окружения. 
Содержимое файлов соответствует значениям этих переменных окружения. Таким образом этот каталог является отличным местом для централизованного добавления
собственных переменных окружения, которые будут унаследованы всеми стартовыми скриптами и сервисами Runit.

Например врт так можно добавить переменную окружения в Docker файле:

    RUN echo Apachai Hopachai > /etc/container_environment/MY_NAME

А так можно проверить что всё сработало как надо::

    $ docker run -t -i <YOUR_NAME_IMAGE> /sbin/my_init -- bash -l
    ...
    *** Running bash -l...
    # echo $MY_NAME
    Apachai Hopachai

**Обработка перевода строк**

Если вы смотрели внимательно, то могли заметить, что команда `echo` добавляет перевод строки. И этот перевод строки попадает в файл. Почему же тогда $MY_NAME не содержит 
перевод строки? Это потому, что `my_init` обрезает завершающий перевод строки. Если вам действительно нужен перевод строки в конце, то вы должны
добавить *ещё один* перевод строки, как тут:

    RUN echo -e "Apachai Hopachai\n" > /etc/container_environment/MY_NAME

<a name="envvar_dumps"></a>
#### Дамп переменных окружения

В то время, как описанный ранее механизм хорош для добавления переменных окружения, сам по себе он не запрещает сервисам
(таким как Nginx) изменять и удалять перемнные окружения из дочерних процессов. Однако механизм `my_init` позволяет упростить
вам запрос к оригинальным значениям переменных.

В процессе запуска, сразу после импорта переменных окружения из `/etc/container_environment`, `my_init` выгружает все свои
переменные окружения (именно все, включая импортированные из `container_environment`, так же как переменные полученные через
`docker run --env`) в следующие места в следующих форматах:

 * `/etc/container_environment`
 * `/etc/container_environment.sh` - выгрузка переменных окружения в формате Bash. Вы можете напрямую подключать этот файл через директиву `source` в Bash скриптах.
 * `/etc/container_environment.json` - выгрузка переменных окружения в формате JSON.

Наличие нескольких форматов упрощает доступ к оригинальным переменным окружения вне зависимости от используемых языков программирования, используемых для ваших приложений и скриптов.

Вот пример сессии Bash, демонстрирующий то, как может выглядеть выгрузка переменных окружения:

    $ docker run -t -i \
      --env FOO=bar --env HELLO='my beautiful world' \
      javister-docker-docker.bintray.io/javister/javister-docker-base:<VERSION>:<VERSION> /sbin/my_init -- \
      bash -l
    ...
    *** Running bash -l...
    # ls /etc/container_environment
    FOO  HELLO  HOME  HOSTNAME  PATH  TERM  container
    # cat /etc/container_environment/HELLO; echo
    my beautiful world
    # cat /etc/container_environment.json; echo
    {"TERM": "xterm", "container": "lxc", "HOSTNAME": "f45449f06950", "HOME": "/root", "PATH": "/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin", "FOO": "bar", "HELLO": "my beautiful world"}
    # source /etc/container_environment.sh
    # echo $HELLO
    my beautiful world

<a name="modifying_envvars"></a>
#### Изменение переменных окружения

Так же есть возможность изменять переменные окружения в `my_init` (а следовательно и переменные окружения во всех дочерних процессах), 
для этого достаточно изменить файлы в `/etc/container_environment`. После каждого раза, когда `my_init` выполняет [очередной скрипт запуска](#running_startup_scripts), 
он перечитывает свои переменные окружения из каталога `/etc/container_environment`, и перевыгружает переменные окружения
в файлы `container_environment.sh` и `container_environment.json`.

Но учитывайте следующее:

 * Модификачия `container_environment.sh` и `container_environment.json` не имеют смысла и быдыт стёрты.
 * Сервисы Runit не могут модифицировать переменные окружения таким же образом. `my_init` учитывает изменения в `/etc/container_environment` только при запуске стартовых скриптов.

<a name="envvar_security"></a>
#### Безопасность

Т.к. переменные окружения потенциально могут иметь чувствительные к безопасновти данные, то каталогом `/etc/container_environment` и его выгрузками в Bash и JSON по
умолчанию владеет пользователь root, и они доступны только для пользователей из группы `docker_env`. Так что любой пользователь, добавленный в эту группу автоматически
получит доступ к этим переменным.

Если вы уверены, что ваши переменные окружения не имеют чувствительных к безопасности данных, то вы можете ослабить права на 
этих каталоге и файлах, сделав их доступными всем для чтения:

    RUN chmod 755 /etc/container_environment
    RUN chmod 644 /etc/container_environment.sh /etc/container_environment.json

<a name="available_envvars"></a>
#### Доступные переменные окружения

В образе javister-docker-base используется ряд переменных окружения:

|Имя                       |Описание|Применимость (когда может устанавливаться)|
|--------------------------|--------|------------------------------------------|
|PUID                      |Идентификатор основного пользователя (system) в контейнере, под которым должны запускаться прикладные сервисы|При запуске контейнера|
|PGID                      |Идентификатор группы основного пользователя (system) в контейнере, под которым должны запускаться прикладные сервисы|При запуске контейнера|
|TERM                      ||При запуске контейнера|
|TZ                        |Временная зона, устанавливаемая при запуске контейнера|При запуске контейнера|
|KILL_PROCESS_TIMEOUT      |Период ожидания завершения основного процесса перед вызовом SIGKILL для него|При запуске контейнера|
|KILL_ALL_PROCESSES_TIMEOUT|Период ожидания завершения всех дочерних процессов перед вызовом SIGKILL для них|При запуске контейнера|
|RPMLIST                   |Список пакетов, необходимые для работы приложения, которые должны установиться при вызове утилиты yum-install|При сборке дочернего образа|
|BUILD_RPMLIST             |Список пакетов, необходимые для сборки дочернего образа, которые должны установиться при вызове утилиты yum-install и удалиться при вызове утилиты yum-clean-build|При сборке дочернего образа|
|BASE_RPMLIST              |Список пакетов, необходимые для сборки базового образа на основе javister-docker-base, которые должны установиться при вызове утилиты yum-install|При сборке дочернего образа|

<a name="container_administration"></a>
## Администрирование контейнера

Одна из основных идей Docker заключается в том, что контейнеры должны не содержать состояния, легко перезапускаться и работать как чёрный ящик. Однако иногда вы
можете попадать в ситуацию, когда вам понадобится зайти внутрь контейнера или запустить в нем какую либо команду для целей разрадотки, исследования или отладки
Данный раздел рассматривает как вы можете управлять контейнерами для этих целей:

<a name="oneshot"></a>
### Запуск одноразовых команд в контейнере

_**Примечание:** Данная секция описывает как запускать команды внутри -нового- контейнера. Чтобы запускать команды внутри имеющегося запущеного коньейнера
смотрите секцию [Запуск команд в существующем, запущенном контейнере](#run_inside_existing_container)._

Обычно, если необходимо создть контейнер, запустить в нём оду команду и немедленно завершить работу работу контейнера, когда работа команды закончится, 
то вы запускаете Docker следующим образом:

    docker run YOUR_IMAGE COMMAND ARGUMENTS...

Однако данный способ запуска имеет минус, заключающийся в том, что система инициализации не будет запущена. Таким образом, при вызове команды `COMMAND`, важные сервисы, такие как
cron и syslog не будут запущены. Так же брошенные дочерние процессы не будут правильно уничтожены, т.к. `COMMAND` будет иметь PID 1.

javister-docker-base обеспечивает возможность для запуска одноразовых команд, при этом решая все озвученные проблемы. Для этого нужно запустить
соманду следующим образом:

    docker run YOUR_IMAGE /sbin/my_init -- COMMAND ARGUMENTS ...

Это выполнит следующие действия:

 * Выполнит все стартовые скрипты, такие как /etc/my_init.d/* и /etc/rc.local.
 * Запустит все runit сервисы.
 * Запустит указанную команду.
 * Когда команда завершится, так же остановит все runit сервисы.

Например:

    $ docker run phusion/baseimage:<VERSION> /sbin/my_init -- ls
    *** Running /etc/rc.local...
    *** Booting runit daemon...
    *** Runit started as PID 80
    *** Running ls...
    bin  boot  dev  etc  home  image  lib  lib64  media  mnt  opt  proc  root  run  sbin  selinux  srv  sys  tmp  usr  var
    *** ls exited with exit code 0.
    *** Shutting down runit daemon (PID 80)...
    *** Killing all processes...

Вы можете решить, что поведение вызова по умолчанию слишком многословно. Или, например, вам просто не нужно вызывать стартовые скрипты. Вы можете изменять всё поведение
путём передачи аргуметов в `my_init`. Выполните `docker run YOUR_IMAGE /sbin/my_init --help` для получения большей информации.

Следующий пример запускает `ls` без запуска стартовых скриптов и с меньшим количеством сообщений, при этом запуская все сервисы runit:

    $ docker run phusion/baseimage:<VERSION> /sbin/my_init --skip-startup-files --quiet -- ls
    bin  boot  dev  etc  home  image  lib  lib64  media  mnt  opt  proc  root  run  sbin  selinux  srv  sys  tmp  usr  var

<a name="run_inside_existing_container"></a>
### Запуск команд в существующем, запущенном контейнере

Вы можете запускать команды внутри существующего, запущенного контейнера, путём запуска команды `docker exec`. Это внутренняя команда Docker,
доступная с версии Docker 1.4. Внутри она использует системные вызовы ядря Linux для вызова команды внутри
контекста контейнера. Больше можно узнать в секции [Вход в контейнер, или запуск соманды внутри него, с помощью `docker exec`](#login_docker_exec).

<a name="login_docker_exec"></a>
### Вход в контейнер, или запуск соманды внутри него, с помощью `docker exec`

Вы можете использовать команду `docker exec` на хост системе Docker для входа в любой контейнер, базирующийся на образе javister-docker-base. Вы так же можете
использовать это для запуска команд внутри запущенного контейнера. `docker exec` работает с использование системных вызовов ядра Linux.

<a name="docker_exec_usage"></a>
#### Примеры использования

Запуск контейнера:

    docker run YOUR_IMAGE

Поиск ID контейнера, который вы только что запустили:

    docker ps

Теперь, имея ID, вы можете использовать `docker exec` для запуска требуемой команды в контейнере. Например для запуска `echo hello world`:

    docker exec YOUR-CONTAINER-ID echo hello world

Для запуска сессии bash внутри контейнера, вы должны передать параметры `-t -i` для получения доступа к терминалу:

    docker exec -t -i YOUR-CONTAINER-ID bash -l

<a name="build"></a>
## Сборка

Для добавление сборки унаследованных образов на Travis CI необходимо скопировать файлы:

* build.sh
* .travis.yml
* config.properties
* downstream.txt

В файле `.travis.yml` необходимо обновить ключи доступа:

* REGISTRY_PASS - пароль для Bintray репозитария. Необходимо использовать API Key из своей учётки
* TRAVIS_TOKEN - ключ для доступа к Travis CI по проотоколу HTTP. Можно получить через вызов CLI: `travis token`

Ключи шифруются следующей командой:

```bash
travis encrypt 'TRAVIS_TOKEN="xxx" REGISTRY_PASS="xxx"'
```

В файле `config.properties` необходимо установить значения, соответствующие текущему проекту.

В файл `downstream.txt` будут добавляться имена проектов, которые зависят от текущего образа для каскадной сборки. Так же необходимо добавить текущий проект в такой 
же файл у проекта того образа, от которого унаследован текущий.
