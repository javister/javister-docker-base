package com.github.javister.docker.testing.base;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;

import java.util.Objects;
import java.util.concurrent.Future;

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
public class JavisterBaseContainerImpl<SELF extends JavisterBaseContainerImpl<SELF>> extends GenericContainer<SELF> implements JavisterBaseContainer<SELF> {
    private static final Logger LOGGER = LoggerFactory.getLogger(JavisterBaseContainerImpl.class);

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
    public JavisterBaseContainerImpl() {
        this(JavisterBaseContainer.getImageTag(JavisterBaseContainerImpl.class, null));
    }

    /**
     * Создание контейнера прямо из базового образа
     * <a href="https://github.com/javister/javister-docker-base">
     * javister-docker-docker.bintray.io/javister/javister-docker-base
     * </a>.
     *
     * @param tag имя тега (версии) образа
     */
    public JavisterBaseContainerImpl(String tag) {
        this(JavisterBaseContainer.getImageRepository(JavisterBaseContainerImpl.class, null), tag);
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
    public JavisterBaseContainerImpl(String dockerImageName, String tag) {
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
    public JavisterBaseContainerImpl(Class<?> testClass) {
        this(JavisterBaseContainer.getImageTag(JavisterBaseContainerImpl.class, null), testClass);
    }

    /**
     * Создание контейнера из автогенерируемого образа по заданному описанию
     * <a href="https://github.com/javister/javister-docker-base">
     * javister-docker-docker.bintray.io/javister/javister-docker-base
     * </a> для проведения JUnit тестирования.
     *
     * <p>Объект класса необходим для нахождения рабочего каталога тестов.
     *
     * @param testClass класс JUnit теста.
     * @param image     описание автогенерируемого образа
     */
    public JavisterBaseContainerImpl(Class<?> testClass, Future<String> image) {
        this(JavisterBaseContainer.getImageTag(JavisterBaseContainerImpl.class, null), testClass);
        setImage(image);
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
    public JavisterBaseContainerImpl(String tag, Class<?> testClass) {
        this(JavisterBaseContainer.getImageRepository(JavisterBaseContainerImpl.class, null), tag, testClass);
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
    public JavisterBaseContainerImpl(String dockerImageName, String tag, Class<?> testClass) {
        super(dockerImageName + ":" + tag);
        this.testClass = testClass;
        initialize();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!super.equals(o)) {
            return false;
        }
        JavisterBaseContainerImpl<?> that = (JavisterBaseContainerImpl<?>) o;
        return Objects.equals(testClass, that.testClass);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), testClass);
    }

    @Override
    @Nullable
    public Class<?> getTestClass() {
        return testClass;
    }

    @NotNull
    @Override
    public String getLogPrefix() {
        return logPrefix;
    }

    @Override
    public void setLogPrefix(@NotNull String logPrefix) {
        this.logPrefix = logPrefix;
    }

    @Override
    public boolean isSuppressSlfLogger() {
        return suppressSlfLogger;
    }

    @Override
    public void setSuppressSlfLogger(boolean suppressSlfLogger) {
        this.suppressSlfLogger = suppressSlfLogger;
    }

    @NotNull
    @Override
    public Slf4jLogConsumer getLogConsumer() {
        return logConsumer;
    }

    @NotNull
    @Override
    public SELF withNetwork(@NotNull Network network) {
        // Форсируем создание сети
        network.getId();
        getInternalDependencies().forEach(it -> it.withNetwork(network));
        return super.withNetwork(network);
    }

    @Override
    public void setNetwork(@NotNull Network network) {
        network.getId();
        getInternalDependencies().forEach(it -> it.setNetwork(network));
        super.setNetwork(network);
    }

    @Override
    protected void configure() {
        super.configure();
        this.withLogConsumer(getLogConsumer());
    }
}
