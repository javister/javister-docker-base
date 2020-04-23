package com.github.javister.docker.testing.base;

import com.github.javister.docker.testing.ExternalLogConsumer;
import com.github.javister.docker.testing.IllegalTestConfigurationException;
import com.github.javister.docker.testing.TestRunException;
import com.github.javister.docker.testing.TestServiceContainer;
import com.github.javister.docker.testing.hack.HostPortWaitStrategyHack;
import com.github.javister.docker.testing.hack.IsRunningStartupCheckStrategyHack;
import org.apache.commons.lang.SystemUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.BaseConsumer;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.WaitStrategyTarget;
import org.testcontainers.lifecycle.Startable;
import org.zeroturnaround.exec.ProcessExecutor;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;


/**
 * Миксин для обёрток над базовым контейнером
 * <a href="https://github.com/javister/javister-docker-base">
 * javister-docker-docker.bintray.io/javister/javister-docker-base
 * </a> и его наследниками.
 *
 * <p>Образ данного контейнера содержит расширеные средства построения кастомных контейнеров и содержин множество
 * дополнительных параметров запуска. Обёртка даёт возможность настраивать некоторые из этих параметров.
 *
 * @param <SELF> параметр, необходимый для организации паттерна fluent API.
 */
@SuppressWarnings({"java:S119", "UnusedReturnValue", "unused"})
public interface JavisterBaseContainer<SELF extends JavisterBaseContainer<SELF>> extends Container<SELF>, AutoCloseable, WaitStrategyTarget, Startable, TestServiceContainer {
    /**
     * Получение объекта класса JUnit теста.
     *
     * @return объект класса JUnit теста, если был задан, или null в противном случае.
     */
    @Nullable
    Class<?> getTestClass();

    /**
     * Получение префикса для выводимых строк лога.
     *
     * <p>Полезно для идентификации логов от разных контейнеров.
     *
     * @return строка префикса строк лога
     */
    @NotNull
    String getLogPrefix();

    /**
     * Установка префикса для выводимых строк лога.
     *
     * <p>Полезно для идентификации логов от разных контейнеров.
     *
     * @param logPrefix строка префикса строк лога
     */
    void setLogPrefix(@NotNull String logPrefix);

    /**
     * Получение флага, обознаяающего необходимость подавления вывода логгирования в SLF4J.
     *
     * @return флаг, обознаяающий необходимость подавления вывода логгирования в SLF4J.
     */
    boolean isSuppressSlfLogger();

    /**
     * Устанока флага, обознаяающего необходимость подавления вывода логгирования в SLF4J.
     *
     * @param suppressSlfLogger значение флага, обознаяающего необходимость подавления вывода логгирования в SLF4J.
     */
    void setSuppressSlfLogger(boolean suppressSlfLogger);

    /**
     * Получение консумера лога контейнера, который перенаправляет вывод в систему логирования теста.
     *
     * @return консумер лога, который перенаправляет вывод в систему логирования теста.
     */
    Slf4jLogConsumer getLogConsumer();

    /**
     * Получение всех консумеров лога контейнера.
     *
     * @return все консумеры лога контейнера.
     */
    @NotNull
    List<Consumer<OutputFrame>> getLogConsumers();

    /**
     * Получение варианта приложения.
     *
     * <p>Одно и тоже приложение может иметь несколько вариантов или версий. Например PostgreSQL может быть версий 9.5,
     * 9.6 и до 12. А Wildfly 8 может быть в варианте с ModeShape или без такового.
     *
     * <p>Данное свойство содержит конкретный вариант, по которому происходит выбор соответствующего образа Docker.
     *
     * <p>Если образ не имеет нескольких вариантов, то должно возвращаться значение null.
     *
     * @return вариант приложения или null, если варианты не предусмотрены.
     */
    @Nullable
    default String getVariant() {
        return null;
    }

    /**
     * Формирует и возвращает путь к рабочему каталогу JUnit тестов по заданному классу тестов.
     * <p>Для Maven проектов путь будет сформирован в виде: <b>${project.path}/target</b>
     *
     * <p>findsecbugs:PATH_TRAVERSAL_IN - путь формируется отностительно URL класса.
     * На первый взгляд никакого криминала тут нет.
     * Могут быть какие-то подводные камни?
     *
     * @param testClass класс, по которому определяется путь к каталогу
     * @return возвращает this для fluent API.
     * @throws IllegalTestConfigurationException если в конструкторе не был указан класс JUnit теста.
     */
    @NotNull
    @SuppressWarnings("findsecbugs:PATH_TRAVERSAL_IN")
    static File getTestPath(@NotNull Class<?> testClass) {
        try {
            return new File(new File(testClass.getProtectionDomain().getCodeSource().getLocation().toURI()), "..");
        } catch (URISyntaxException e) {
            throw new IllegalTestConfigurationException("Ошибка определения каталога сборки проекта.", e);
        }
    }

    /**
     * Удаляет каталог, который монтируется к контейнеру во время выполнения теста.
     *
     * @throws IOException если удаление каталога не удалось.
     */
    default void deleteTestDir() throws IOException {
        deleteDir(getTestVolumePath());
    }

    /**
     * Удаляет указанный каталог.
     *
     * @param dir каталог, который необходимо удалить.
     * @throws IOException если удаление каталога не удалось.
     */
    static void deleteDir(File dir) throws IOException {
        if (dir != null) {
            deleteDir(dir.getAbsolutePath());
        }
    }

    /**
     * Удаляет указанный каталог.
     *
     * @param dir каталог, который необходимо удалить.
     * @throws IOException если удаление каталога не удалось.
     */
    static void deleteDir(String dir) throws IOException {
        if (dir == null) {
            return;
        }
        Path path = Paths.get(dir);
        if (Files.exists(path)) {
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }

    /**
     * Производит настройку контейнера по уморчанию.
     *
     * <p> Данный метод должен вызываться в конструкторе имплементирующего класса.
     */
    default void initialize() {
        try {
            this
                    .withLang("ru_RU.UTF-8")
                    .withStartupCheckStrategy(new IsRunningStartupCheckStrategyHack())
                    .setWaitStrategy(new HostPortWaitStrategyHack());
            if (SystemUtils.IS_OS_LINUX) {
                String puid = new ProcessExecutor().command("id", "-u")
                        .readOutput(true).execute()
                        .outputUTF8()
                        .trim();
                String pgid = new ProcessExecutor().command("id", "-g")
                        .readOutput(true).execute()
                        .outputUTF8()
                        .trim();
                this
                        .withPUID(puid)
                        .withPGID(pgid)
                        .autoHttpProxy(true);
            }
        } catch (InterruptedException | TimeoutException | IOException e) {
            throw new IllegalTestConfigurationException("Ошибка подготовки окружения.", e);
        }
    }

    /**
     * Подавляет вывод в лог приложения.
     *
     * @param suppressed подавлять ли вывод в лог приложения
     * @return возвращает this для fluent API.
     */
    @NotNull
    default SELF withSlfLogSuppressed(boolean suppressed) {
        setSuppressSlfLogger(suppressed);
        getLogConsumers().remove(getLogConsumer());
        if (!suppressed) {
            getLogConsumers().add(getLogConsumer());
        }
        getInternalDependencies().forEach(it -> it.withSlfLogSuppressed(isSuppressSlfLogger()));
        return self();
    }

    /**
     * Задаёт префикс лога Docker контейнера.
     * <p>Если запускается система из нескольких контейнеров, то с момощью данного префикса удобно помечать какой
     * лог какому контейнеру принадлежит.
     *
     * @param prefix префикс лога. Желательно задавать имя приложения
     * @return возвращает this для fluent API.
     */
    @NotNull
    default SELF withLogPrefix(String prefix) {
        getLogConsumer().withPrefix(prefix);
        setLogPrefix(prefix);
        return self();
    }

    /**
     * Задание настроки очистки лога контейнера от ANSI кодов.
     * <p>По умолчанию выводимый лог очищается от ANSI кодов (значение true). Это в частности убирает цветовую подсветку
     * из лога. Если цвета требуется сохранить, то необходимо установить данное значение в false.
     *
     * @param remove true для удаление ANSI кодов (по умолчанию) и false в противном случае.
     * @return возвращает this для fluent API.
     */
    @NotNull
    default SELF withRemoveAnsiCodes(boolean remove) {
        getLogConsumer().withRemoveAnsiCodes(remove);
        return self();
    }

    /**
     * Устанавливает ID пользователя Linux по умолчанию в контейнере.
     * <p>Устанавливать ID желательно в значение того пользователя, под которым запускаются тесты.
     * Ряд контейнеров построены так, что указание данной опции позволяет сохранить доступ к файлам (в примонтированных
     * каталогах), формируемым в процессе работы контейнера. В противном случае доступ к файлам сможет получить только
     * пользователь root.
     * <p>Всё выше сказанное относится только к пользователям Linux.
     *
     * @param puid ID, которое необходимо выставить для внутреннего пользователя контейнера.
     * @return возвращает this для fluent API.
     */
    @NotNull
    default SELF withPUID(String puid) {
        return this.withEnv("PUID", puid);
    }

    /**
     * Устанавливает ID группы пользователя Linux по умолчанию в контейнере.
     * <p>Устанавливать ID желательно в значение группы того пользователя, под которым запускаются тесты.
     * Ряд контейнеров построены так, что указание данной опции позволяет сохранить доступ к файлам (в примонтированных
     * каталогах), формируемым в процессе работы контейнера. В противном случае доступ к файлам сможет получить только
     * пользователь root.
     * <p>Всё выше сказанное относится только к пользователям Linux.
     *
     * @param pgid ID, которое необходимо выставить для группы внутреннего пользователя контейнера.
     * @return возвращает this для fluent API.
     */
    @NotNull
    default SELF withPGID(String pgid) {
        return this.withEnv("PGID", pgid);
    }

    /**
     * Устанавливает адрес HTTP proxy внутри контейнера в формате Linux.
     *
     * @param httpProxy адрес HTTP proxy в формате Linux.
     * @return возвращает this для fluent API.
     */
    @NotNull
    default SELF withHttpProxy(String httpProxy) {
        return this
                .withEnv("http_proxy", httpProxy)
                .withEnv("https_proxy", httpProxy);
    }

    /**
     * Устанавливает исключения HTTP proxy внутри контейнера в формате Linux.
     *
     * @param noProxy исключения HTTP proxy в формате Linux.
     * @return возвращает this для fluent API.
     */
    @NotNull
    default SELF withNoProxy(String noProxy) {
        return this.withEnv("no_proxy", noProxy);
    }

    /**
     * Автоопределение параметров HTTP Proxy из переменных окружения системы.
     *
     * @param enable если true, то установить настройки proxy из переменных окружения системы. В противном случае очистить настройки proxy
     * @return возвращает this для fluent API.
     */
    @NotNull
    default SELF autoHttpProxy(boolean enable) {
        if (enable) {
            this
                    .withHttpProxy(System.getenv("http_proxy"))
                    .withNoProxy(System.getenv("no_proxy"));
        } else {
            this
                    .withHttpProxy(null)
                    .withNoProxy(null);
        }
        return self();
    }

    /**
     * Устанавливает язык и локаль и кодировку внутри контейнера.
     * <p>Данное значение необходимо устанавливать в случае, когда ожидается, что приложение может выводить логи не в
     * английской локали, или использовать спицифичные настройки локали для форматировани дат, времени, денежных единиц
     * и т.п.
     * <p>Пример значения: <b>ru_RU.UTF-8</b>
     *
     * @param lang название локали и кодировки.
     * @return возвращает this для fluent API.
     */
    @NotNull
    default SELF withLang(String lang) {
        return this.withEnv("LANG", lang);
    }

    /**
     * Добавляет логгер для вывода логов во внешнюю систему. Включай логи контейнеров-зависимостей.
     * <p>Например этот метод может использоваться для перенаправления вывода контейнера
     * и его зависимостей в лог задачи сборки на Jenkins.
     *
     * @param consumer приёмник логов
     * @return возвращает this для fluent API.
     */
    @NotNull
    default SELF withExternalLogConsumer(Consumer<OutputFrame> consumer) {
        getInternalDependencies().forEach(it -> it.withExternalLogConsumer(consumer));
        return withLogConsumer(new LogConsumerDecorator(getLogPrefix(), consumer));
    }

    /**
     * Добавляет биндинг файловой системы, относительно каталога {@link JavisterBaseContainer#getTestVolumePath()}.
     * <p>Если каталог {@link JavisterBaseContainer#getTestVolumePath()} не определён - вернёт null.
     *
     * @param hostPath      путь на хосте
     * @param containerPath путь внутри контейнера
     * @return возвращает this для fluent API.
     */
    @NotNull
    default SELF withRelativeFileSystemBind(String hostPath, String containerPath) {
        File path = getTestVolumePath();
        if (path != null) {
            this.withFileSystemBind(path.toString() + "/" + hostPath, containerPath);
        }
        return self();
    }

    /**
     * Добавляет биндинг файловой системы, относительно каталога {@link JavisterBaseContainer#getTestVolumePath()}.
     * <p>Если каталог {@link JavisterBaseContainer#getTestVolumePath()} не определён - вернёт null.
     *
     * @param hostPath      путь на хосте
     * @param containerPath путь внутри контейнера
     * @param mode          режым монтирования
     * @return возвращает this для fluent API.
     */
    @NotNull
    default SELF withRelativeFileSystemBind(String hostPath, String containerPath, BindMode mode) {
        File path = getTestVolumePath();
        if (path != null) {
            this.withFileSystemBind(path.toString() + "/" + hostPath, containerPath, mode);
        }
        return self();
    }

    /**
     * Задаёт логин пользователя по умолчанию.
     * <p>Если этот параметр не задан, то пользователь по умолчанию имеет логин {@code system}.
     * <p>Данный параметр переопределяет логин на указанный. В качестве одного из примеров, когда
     * это может быть необходимо: образ с PostgreSQL, которому требуется наличие системного
     * пользователя {@code postgres}.
     *
     * @param username логин пользователя по умолчанию.
     * @return возвращает this для fluent API.
     */
    default SELF withUserName(String username) {
        withEnv("PUSER", username);
        return self();
    }

    /**
     * Формирует и возвращает путь к каталогу, который необходимо примонтировать при выполнении JUnit тестов.
     * <p>Для Maven проектов путь будет сформирован в виде: <b>${project.path}/target/docker-&lt;junit-class-name&gt;</b>
     * <p>Данный путь може быть сформирован только если был указан класс JUnit теста в конструкторе.
     *
     * <p>findsecbugs:PATH_TRAVERSAL_IN - путь формируется отностительно URL класса.
     * На первый взгляд никакого криминала тут нет.
     * Могут быть какие-то подводные камни?
     *
     * @return возвращает this для fluent API.
     * @throws IllegalTestConfigurationException если в конструкторе не был указан класс JUnit теста.
     */
    @Nullable
    @Override
    @SuppressWarnings("findsecbugs:PATH_TRAVERSAL_IN")
    default File getTestVolumePath() {
        try {
            if (System.getenv().containsKey("DOCKER_HOST") || getTestClass() == null) {
                return null;
            }
            return new File(
                    new File(getTestClass().getProtectionDomain().getCodeSource().getLocation().toURI()),
                    "../" +
                            "docker-" +
                            getTestClass().getSimpleName() +
                            (getVariant() != null ? "-" + getVariant() : "")
            );
        } catch (URISyntaxException e) {
            throw new IllegalTestConfigurationException("Ошибка определения каталога сборки проекта.", e);
        }
    }

    /**
     * Формирует и возвращает путь к рабочему каталогу JUnit тестов.
     * <p>Для Maven проектов путь будет сформирован в виде: <b>${project.path}/target</b>
     * <p>Данный путь може быть сформирован только если был указан класс JUnit теста в конструкторе.
     *
     * @return возвращает this для fluent API.
     * @throws IllegalTestConfigurationException если в конструкторе не был указан класс JUnit теста.
     */
    @Nullable
    @Override
    default File getTestPath() {
        return getTestClass() != null ? JavisterBaseContainer.getTestPath(getTestClass()) : null;
    }

    /**
     * Ожидание доступности подключения из контейнера по заданному адресу и порту в течении заданного количества секунд.
     *
     * @param host    адрес по которому ожидать подключение
     * @param port    порт по которому ожидать подключение
     * @param seconds время, в течении которого ожидать подключения
     * @return возвращает true, если удалось дождаться подключения и false в случае таймаута
     */
    default boolean waitConnectionOpen(@NotNull String host, int port, int seconds) throws IOException, InterruptedException {
        ExecResult execResult = execInContainer("wait4tcp", "-w", Integer.toString(seconds), host, Integer.toString(port));
        return execResult.getExitCode() == 0;
    }

    /**
     * Ожидание закрытия подключения из контейнера по заданному адресу и порту в течении заданного количества секунд.
     *
     * @param host    адрес по которому ожидать закрытия подключение
     * @param port    порт по которому ожидать закрытия подключение
     * @param seconds время, в течении которого ожидать закрытия подключения
     * @return возвращает true, если удалось дождаться закрытия подключения и false в случае таймаута
     */
    default boolean waitConnectionClose(@NotNull String host, int port, int seconds) throws IOException, InterruptedException {
        ExecResult execResult = execInContainer("wait4tcp", "-w", Integer.toString(seconds), host, Integer.toString(port));
        return execResult.getExitCode() == 0;
    }

    /**
     * Утилитный метод преобразования boolean значение в значения on/off.
     *
     * @param value значение, требующее для преобразования
     * @return <b>on</b>, если value равно true и <b>off</b> в противном случае.
     */
    @NotNull
    @Contract(pure = true)
    static String boolToOnOff(boolean value) {
        return value ? "on" : "off";
    }

    @NotNull
    @Override
    SELF withNetwork(@NotNull Network network);

    void setNetwork(@NotNull Network network);

    /**
     * Получение списка внутренних контейнеров, от которых зависит данный контейнер.
     *
     * <p>Подразумевается, что жизненным циклом этих зависимостей (запуском, остановкой, сетями и т.п.) управляет данный контейнер.
     *
     * <p>Если наследник этого класса зависит от каких-либо других контейнеров и запускает их при старте, то он должен перекрыть данный
     * метод и вернуть список этих контейнеров.
     *
     * <p>В противном случае как минимум не будет работать автоматическое поднятие стендов на Jenkins.
     *
     * @return список зависимостей
     */
    @NotNull
    default List<JavisterBaseContainer<SELF>> getInternalDependencies() {
        return Collections.emptyList();
    }

    /**
     * Получение списка внешних контейнеров, от которых зависит данный контейнер.
     *
     * <p>Подразумевается, что жизненным циклом этих зависимостей (запуском, остановкой, сетями и т.п.) данный контейнер не управляет,
     * а этим занимается пользовательский код.
     *
     * <p>Если наследник этого класса зависит от каких-либо других контейнеров и запускает их при старте, то он должен перекрыть данный
     * метод и вернуть список этих контейнеров.
     *
     * <p>В противном случае как минимум не будет работать автоматическое поднятие стендов на Jenkins.
     *
     * @return список зависимостей
     */
    @NotNull
    default List<JavisterBaseContainer<SELF>> getExternalDependencies() {
        return Collections.emptyList();
    }

    /**
     * Ролучение идентификатора образа (хеша) для указанной обёртки с учётом её версии и варианта.
     *
     * @param clazz   класс обёртки контейнера, для которого ищятся метаданные.
     * @param variant вариант образа, для которого ищятся метаданные. Или null, если вариантов не предусмотренно.
     * @param <SELF>  параметр, необходимый для организации паттерна fluent API.
     * @return идентификатор образа (хеша) для указанной обёртки с учётом её версии и варианта.
     */
    @NotNull
    static <SELF extends JavisterBaseContainer<SELF>> String getImageId(
            @NotNull Class<SELF> clazz,
            @Nullable String variant) {
        return getImageMeta(clazz, variant, "image-id");
    }

    /**
     * Получение полного имени образа для указанной обёртки с учётом её версии и варианта.
     *
     * @param clazz   класс обёртки контейнера, для которого ищятся метаданные.
     * @param variant вариант образа, для которого ищятся метаданные. Или null, если вариантов не предусмотренно.
     * @param <SELF>  параметр, необходимый для организации паттерна fluent API.
     * @return полне имя образа для указанной обёртки с учётом её версии и варианта.
     */
    @NotNull
    static <SELF extends JavisterBaseContainer<SELF>> String getImageName(
            @NotNull Class<SELF> clazz,
            @Nullable String variant) {
        return getImageMeta(clazz, variant, "image-name");
    }

    /**
     * Получение имени Docker образа (без тега) для указанной обёртки с учётом её версии и варианта.
     *
     * @param clazz   класс обёртки контейнера, для которого ищятся метаданные.
     * @param variant вариант образа, для которого ищятся метаданные. Или null, если вариантов не предусмотренно.
     * @param <SELF>  параметр, необходимый для организации паттерна fluent API.
     * @return имя Docker образа (без тега) для указанной обёртки с учётом её версии и варианта.
     */
    @NotNull
    static <SELF extends JavisterBaseContainer<SELF>> String getImageRepository(
            @NotNull Class<SELF> clazz,
            @Nullable String variant) {
        return getImageMeta(clazz, variant, "repository");
    }

    /**
     * Получение тега Docker образа для указанной обёртки с учётом её версии и варианта.
     *
     * @param clazz   класс обёртки контейнера, для которого ищятся метаданные.
     * @param variant вариант образа, для которого ищятся метаданные. Или null, если вариантов не предусмотренно.
     * @param <SELF>  параметр, необходимый для организации паттерна fluent API.
     * @return тег Docker образа для указанной обёртки с учётом её версии и варианта.
     */
    @NotNull
    static <SELF extends JavisterBaseContainer<SELF>> String getImageTag(
            @NotNull Class<SELF> clazz,
            @Nullable String variant) {
        return getImageMeta(clazz, variant, "tag");
    }

    /**
     * Поучение конкретного знячения метаданных образа по классу обёртки и варианту.
     * <p>Метаданные формируются при сборке образа с помощью
     * <a href="https://github.com/spotify/dockerfile-maven">dockerfile-maven-plugin</a>.
     *
     * @param clazz        класс обёртки контейнера, для которого ищятся метаданные.
     * @param variant      вариант образа, для которого ищятся метаданные. Или null, если вариантов не предусмотренно.
     * @param metaFileName имя файла метаданных, из которого необходимо извлеч значение.
     * @param <SELF>       параметр, необходимый для организации паттерна fluent API.
     * @return конкретное значение метаданных.
     */
    @NotNull
    static <SELF extends JavisterBaseContainer<SELF>> String getImageMeta(
            @NotNull Class<SELF> clazz,
            @Nullable String variant,
            @NotNull String metaFileName) {
        try (InputStream response = clazz.getResourceAsStream(getImageCoordinate(clazz, variant) + metaFileName);
             Scanner scanner = new Scanner(response)) {
            return scanner.nextLine();
        } catch (IOException e) {
            throw new TestRunException("Can't get the Docker image coordinates: " + metaFileName, e);
        }
    }

    /**
     * Получение пути в class path в метаданным образа.
     * <p>Путь формируется из записей файла {@code image.properties}, лежащего рядом с указанным классом,
     * и указанного варианта.
     * <p>Файл {@code image.properties} должен иметь следующую структуру:
     * <pre>
     * groupId=${docker.image.groupId}
     * artifactId=${docker.image.artifactId}
     * </pre>
     * Макроподстановки должны заполняться при сборке Maven проекта.
     *
     * @param clazz   класс обёртки контейнера, для которого ищятся метаданные.
     * @param variant вариант образа, для которого ищятся метаданные. Или null, если вариантов не предусмотренно.
     * @param <SELF>  параметр, необходимый для организации паттерна fluent API.
     * @return путь в class path до методанных
     */
    @NotNull
    static <SELF extends JavisterBaseContainer<SELF>> String getImageCoordinate(
            @NotNull Class<SELF> clazz,
            @Nullable String variant) {
        try (InputStream propStream = clazz.getResourceAsStream("image.properties")) {
            Properties props = new Properties();
            props.load(propStream);
            return "/META-INF/docker/"
                    + props.getProperty("groupId")
                    + "/" + props.getProperty("artifactId")
                    + (variant == null || variant.isEmpty() ? "" : "-" + variant)
                    + "/";
        } catch (IOException e) {
            throw new TestRunException("Can't get the Docker image coordinates", e);
        }
    }

    /**
     * Служебный класс для обеспечения возможности подключения внешних обработчиков лога контейнера.
     */
    class LogConsumerDecorator extends BaseConsumer<ExternalLogConsumer> {
        private final String prefix;
        private final Consumer<OutputFrame> externalConsumer;

        public LogConsumerDecorator(@NotNull String prefix, @NotNull Consumer<OutputFrame> externalConsumer) {
            this.prefix = prefix;
            this.externalConsumer = externalConsumer;
        }

        @Override
        public void accept(@NotNull OutputFrame outputFrame) {
            externalConsumer.accept(new OutputFrameDecorator(prefix, outputFrame));
        }
    }

    /**
     * Обработчик лога контейнера, добавляющий префик контейнера к записям лога.
     */
    class OutputFrameDecorator extends OutputFrame {
        private final String prefix;

        public OutputFrameDecorator(@NotNull String prefix, @NotNull OutputFrame outputFrame) {
            super(outputFrame.getType(), outputFrame.getBytes());
            this.prefix = prefix;
        }

        @NotNull
        @Override
        public String getUtf8String() {
            return "[" + prefix + "] " + super.getUtf8String();
        }
    }
}
