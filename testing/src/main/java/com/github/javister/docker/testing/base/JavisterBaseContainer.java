package com.github.javister.docker.testing.base;

import com.github.javister.docker.testing.ExternalLogConsumer;
import com.github.javister.docker.testing.IllegalTestConfigurationException;
import com.github.javister.docker.testing.TestRunException;
import com.github.javister.docker.testing.TestServiceContainer;
import com.github.javister.docker.testing.hack.HostPortWaitStrategyHack;
import com.github.javister.docker.testing.hack.IsRunningStartupCheckStrategyHack;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.SystemUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.BaseConsumer;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.zeroturnaround.exec.ProcessExecutor;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

/**
 * Обёртка над базовым контейнером
 * <a href="https://github.com/javister/javister-docker-base">
 * javister-docker-docker.bintray.io/javister/javister-docker-base
 * </a>.
 *
 * <p>Образ данного контейнера содержит расширеные средства построения кастомных контейнеров и содержин множество
 * дополнительных параметров запуска. Обёртка даёт возможность настраивать некоторые из этих параметров.
 *
 * <p>squid:S00119 - данная нотация диктуется библиотекой testcontainers.
 *
 * @param <SELF> параметр, необходимый для организации паттерна fluent API.
 */
@SuppressWarnings({"squid:S00119", "WeakerAccess", "unused", "UnusedReturnValue"})
public class JavisterBaseContainer<SELF extends JavisterBaseContainer<SELF>> extends GenericContainer<SELF> implements TestServiceContainer {
    private static final Logger LOGGER = LoggerFactory.getLogger(JavisterBaseContainer.class);

    private Class<?> testClass;
    protected String logPrefix = "DOCKER";
    private boolean suppressSlfLogger = false;
    private final Slf4jLogConsumer logConsumer = new Slf4jLogConsumer(LOGGER).withPrefix(logPrefix).withRemoveAnsiCodes(false);

    /**
     * Создание контейнера прямо из базового образа
     * <a href="https://github.com/javister/javister-docker-base">
     * javister-docker-docker.bintray.io/javister/javister-docker-base
     * </a>.
     */
    public JavisterBaseContainer() {
        this(getImageTag(JavisterBaseContainer.class));
    }

    /**
     * Создание контейнера прямо из базового образа
     * <a href="https://github.com/javister/javister-docker-base">
     * javister-docker-docker.bintray.io/javister/javister-docker-base
     * </a>.
     *
     * @param tag имя тега (версии) образа
     */
    public JavisterBaseContainer(String tag) {
        this(getImageRepository(JavisterBaseContainer.class), tag);
    }

    /**
     * Создание контейнера из какого-либо образа, унаследованного от образа
     * <a href="https://github.com/javister/javister-docker-base">
     * javister-docker-docker.bintray.io/javister/javister-docker-base
     * </a>.
     *
     * @param dockerImageName имя образа из которого необходимо создать контейнер.
     * @param tag             имя тега (версии) образа
     */
    public JavisterBaseContainer(String dockerImageName, String tag) {
        super(dockerImageName + ":" + tag);
        initialize();
    }

    /**
     * Создание контейнера прямо из базового образа
     * <a href="https://github.com/javister/javister-docker-base">
     * javister-docker-docker.bintray.io/javister/javister-docker-base
     * </a> для проведения JUnit тестирования.
     *
     * <p>Объект класса необходим для нахождения рабочего каталога тестов.
     *
     * @param testClass класс JUnit теста.
     */
    public JavisterBaseContainer(Class<?> testClass) {
        this(getImageTag(JavisterBaseContainer.class), testClass);
    }

    /**
     * Создание контейнера прямо из базового образа
     * <a href="https://github.com/javister/javister-docker-base">
     * javister-docker-docker.bintray.io/javister/javister-docker-base
     * </a> для проведения JUnit тестирования.
     *
     * <p>Объект класса необходим для нахождения рабочего каталога тестов.
     *
     * @param tag       имя тега (версии) образа
     * @param testClass класс JUnit теста.
     */
    public JavisterBaseContainer(String tag, Class<?> testClass) {
        this(getImageRepository(JavisterBaseContainer.class), tag, testClass);
    }

    /**
     * Создание контейнера из какого-либо образа, унаследованного от образа
     * <a href="https://github.com/javister/javister-docker-base">
     * javister-docker-docker.bintray.io/javister/javister-docker-base
     * </a> для проведения JUnit тестирования.
     *
     * <p>Объект класса необходим для нахождения рабочего каталога тестов.
     *
     * @param dockerImageName имя образа из которого необходимо создать контейнер.
     * @param tag             имя тега (версии) образа
     * @param testClass       класс JUnit теста.
     */
    public JavisterBaseContainer(String dockerImageName, String tag, Class<?> testClass) {
        super(dockerImageName + ":" + tag);
        this.testClass = testClass;
        initialize();
    }

    private static Future<String> wrapName(String name) {
        return new Future<String>() {
            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                return false;
            }

            @Override
            public boolean isCancelled() {
                return false;
            }

            @Override
            public boolean isDone() {
                return true;
            }

            @Override
            public String get() {
                return name;
            }

            @Override
            public String get(long timeout, @NotNull TimeUnit unit) {
                return name;
            }
        };
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!super.equals(o)) {
            return false;
        }
        JavisterBaseContainer<?> that = (JavisterBaseContainer<?>) o;
        return Objects.equals(testClass, that.testClass);
    }

    @Override
    public int hashCode() {

        return Objects.hash(super.hashCode(), testClass);
    }

    private void initialize() {
        try {
            this.withLang("ru_RU.UTF-8").withLogConsumer(logConsumer).setWaitStrategy(new HostPortWaitStrategyHack());
            this.withImagePullPolicy(it -> false).withStartupCheckStrategy(new IsRunningStartupCheckStrategyHack());
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
                        .withHttpProxy(System.getenv("http_proxy"))
                        .withNoProxy(System.getenv("no_proxy"));
            }
        } catch (InterruptedException | TimeoutException | IOException e) {
            throw new IllegalTestConfigurationException("Ошибка подготовки окружения.", e);
        }
    }

    /**
     * Получение объекта класса JUnit теста.
     *
     * @return объект класса JUnit теста, если был задан, или null в противном случае.
     */
    @Nullable
    public Class<?> getTestClass() {
        return testClass;
    }

    /**
     * Подавляет вывод в лог приложения.
     *
     * @param suppressed подавлять ли вывод в лог приложения
     * @return возвращает this для fluent API.
     */
    public final SELF withSlfLogSuppressed(boolean suppressed) {
        suppressSlfLogger = suppressed;
        getLogConsumers().remove(logConsumer);
        if (!suppressed) {
            getLogConsumers().add(logConsumer);
        }
        getInternalDependencies().forEach(it -> it.withSlfLogSuppressed(suppressSlfLogger));
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
    public final SELF withLogPrefix(String prefix) {
        logConsumer.withPrefix(prefix);
        logPrefix = prefix;
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
    public SELF withRemoveAnsiCodes(boolean remove) {
        logConsumer.withRemoveAnsiCodes(remove);
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
    public SELF withPUID(String puid) {
        this.withEnv("PUID", puid);
        return self();
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
    public SELF withPGID(String pgid) {
        this.withEnv("PGID", pgid);
        return self();
    }

    /**
     * Устанавливает адрес HTTP proxy внутри контейнера в формате Linux.
     *
     * @param httpProxy адрес HTTP proxy в формате Linux.
     * @return возвращает this для fluent API.
     */
    @NotNull
    public SELF withHttpProxy(String httpProxy) {
        this.withEnv("http_proxy", httpProxy);
        return self();
    }

    /**
     * Устанавливает исключения HTTP proxy внутри контейнера в формате Linux.
     *
     * @param noProxy исключения HTTP proxy в формате Linux.
     * @return возвращает this для fluent API.
     */
    @NotNull
    public SELF withNoProxy(String noProxy) {
        this.withEnv("no_proxy", noProxy);
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
    public SELF withLang(String lang) {
        this.withEnv("LANG", lang);
        return self();
    }

    /**
     * Добавляет логгер для вывода логов во внешнюю систему. Включай логи контейнеров-зависимостей.
     * <p>Например этот метод может использоваться для перенаправления вывода контейнера
     * и его зависимостей в лог задачи сборки на Jenkins.
     *
     * @param consumer приёмник логов
     * @return возвращает this для fluent API.
     */
    public SELF withExternalLogConsumer(Consumer<OutputFrame> consumer) {
        getInternalDependencies().forEach(it -> it.withExternalLogConsumer(consumer));
        return super.withLogConsumer(new LogConsumerDecorator(logPrefix, consumer));
    }

    /**
     * Добавляет биндинг файловой системы, относительно каталога {@link JavisterBaseContainer#getTestVolumePath()}.
     * <p>Если каталог {@link JavisterBaseContainer#getTestVolumePath()} не определён - вернёт null.
     *
     * @param hostPath      путь на хосте
     * @param containerPath путь внутри контейнера
     * @return возвращает this для fluent API.
     */
    public SELF withRelativeFileSystemBind(String hostPath, String containerPath) {
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
    public SELF withRelativeFileSystemBind(String hostPath, String containerPath, BindMode mode) {
        File path = getTestVolumePath();
        if (path != null) {
            this.withFileSystemBind(path.toString() + "/" + hostPath, containerPath, mode);
        }
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
    public final File getTestVolumePath() {
        try {
            if (System.getenv().containsKey("DOCKER_HOST")) {
                return null;
            }
            return new File(new File(testClass.getProtectionDomain().getCodeSource().getLocation().toURI()),
                    "../" + FilenameUtils.getName("docker-" + testClass.getSimpleName()));
        } catch (URISyntaxException e) {
            throw new IllegalTestConfigurationException("Ошибка определения каталога сборки проекта.", e);
        }
    }

    /**
     * Формирует и возвращает путь к рабочему каталогу JUnit тестов.
     * <p>Для Maven проектов путь будет сформирован в виде: <b>${project.path}/target</b>
     * <p>Данный путь може быть сформирован только если был указан класс JUnit теста в конструкторе.
     *
     * <p>findsecbugs:PATH_TRAVERSAL_IN - путь формируется отностительно URL класса.
     * На первый взгляд никакого криминала тут нет.
     * Могут быть какие-то подводные камни?
     *
     * @return возвращает this для fluent API.
     * @throws IllegalTestConfigurationException если в конструкторе не был указан класс JUnit теста.
     */
    @NotNull
    @Override
    @SuppressWarnings("findsecbugs:PATH_TRAVERSAL_IN")
    public final File getTestPath() {
        try {
            return new File(new File(testClass.getProtectionDomain().getCodeSource().getLocation().toURI()), "..");
        } catch (URISyntaxException e) {
            throw new IllegalTestConfigurationException("Ошибка определения каталога сборки проекта.", e);
        }
    }

    /**
     * Ожидание доступности подключения из контейнера по заданному адресу и порту в течении заданного количества секунд.
     *
     * @param host    адрес по которому ожидать подключение
     * @param port    порт по которому ожидать подключение
     * @param seconds время, в течении которого ожидать подключения
     * @return возвращает true, если удалось дождаться подключения и false в случае таймаута
     */
    public boolean waitConnectionOpen(String host, int port, int seconds) throws IOException, InterruptedException {
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
    public boolean waitConnectionClose(String host, int port, int seconds) throws IOException, InterruptedException {
        ExecResult execResult = execInContainer("wait4tcp", "-c", "-w", Integer.toString(seconds), host, Integer.toString(port));
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
    protected static String boolToOnOff(boolean value) {
        return value ? "on" : "off";
    }

    @Override
    public SELF withNetwork(Network network) {
        network.getId();
        getInternalDependencies().forEach(it -> it.withNetwork(network));
        return super.withNetwork(network);
    }

    @Override
    public void setNetwork(Network network) {
        network.getId();
        getInternalDependencies().forEach(it -> it.setNetwork(network));
        super.setNetwork(network);
    }

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
    protected List<JavisterBaseContainer<SELF>> getInternalDependencies() {
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
    protected List<JavisterBaseContainer<SELF>> getExternalDependencies() {
        return Collections.emptyList();
    }

    protected static <SELF extends JavisterBaseContainer<SELF>> String getImageId(Class<SELF> clazz) {
        return getImageMeta(clazz, "image-id");
    }

    protected static <SELF extends JavisterBaseContainer<SELF>> String getImageName(Class<SELF> clazz) {
        return getImageMeta(clazz, "image-name");
    }

    protected static <SELF extends JavisterBaseContainer<SELF>> String getImageRepository(Class<SELF> clazz) {
        return getImageMeta(clazz, "repository");
    }

    protected static <SELF extends JavisterBaseContainer<SELF>> String getImageTag(Class<SELF> clazz) {
        return getImageMeta(clazz, "tag");
    }

    protected static <SELF extends JavisterBaseContainer<SELF>> String getImageMeta(Class<SELF> clazz, String metaFileName) {
        try (BufferedReader reader =
                     new BufferedReader(
                             new InputStreamReader(
                                     clazz.getResourceAsStream(getImageCoordinate(clazz) + metaFileName)
                             )
                     )
        ) {
            return reader.readLine();
        } catch (IOException e) {
            throw new TestRunException("Can't get the Docker image coordinates: " + metaFileName, e);
        }
    }

    protected static <SELF extends JavisterBaseContainer<SELF>> String getImageCoordinate(Class<SELF> clazz) {
        try (InputStream propStream = clazz.getResourceAsStream("image.properties")) {
            Properties props = new Properties();
            props.load(propStream);
            return "/META-INF/docker/" + props.getProperty("groupId") + "/" + props.getProperty("artifactId") + "/";
        } catch (IOException e) {
            throw new TestRunException("Can't get the Docker image coordinates", e);
        }
    }

    private static class LogConsumerDecorator extends BaseConsumer<ExternalLogConsumer> {
        private final String prefix;
        private final Consumer<OutputFrame> externalConsumer;

        public LogConsumerDecorator(String prefix, Consumer<OutputFrame> externalConsumer) {
            this.prefix = prefix;
            this.externalConsumer = externalConsumer;
        }

        @Override
        public void accept(OutputFrame outputFrame) {
            externalConsumer.accept(new OutputFrameDecorator(prefix, outputFrame));
        }
    }

    private static class OutputFrameDecorator extends OutputFrame {
        private final String prefix;

        public OutputFrameDecorator(String prefix, OutputFrame outputFrame) {
            super(outputFrame.getType(), outputFrame.getBytes());
            this.prefix = prefix;
        }

        @Override
        public String getUtf8String() {
            return "[" + prefix + "] " + super.getUtf8String();
        }
    }
}
