package com.github.javister.docker.testing;

import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.BrowserType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.BrowserWebDriverContainer;
import org.testcontainers.shaded.org.zeroturnaround.exec.stream.slf4j.Slf4jStream;
import org.zeroturnaround.exec.ProcessExecutor;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Расширение стандартной обёртки над Selenium WebDrive, добавляющее возможность автоматического подключения к
 * инфраструктуре с приложением на базе НТП.
 */
public class JavisterWebDriverContainer extends BrowserWebDriverContainer {
    private static final boolean USE_LOCAL_X_SERVER = Boolean.parseBoolean(System.getProperty("stand.use.local.x.server", ""));
    private TestServiceContainer appContainer;
    private DesiredCapabilities desiredCapabilities;
    private VncRecordingMode recordingMode;

    /**
     * Создаёт контейнер, подключенный к заданному приложению и с заданными характеристиками.
     *
     * <p>В результате будет создан контейнер с браузером, указанным в @{code desiredCapabilities}.
     * Запись видео будет проводиться в режиме, указанном в параметре {@code recordingMode}.
     * Контейнер будет подключен к виртуальной сети переданного приложения.
     *
     * @param appContainer        обёртка над приложением, к которому необходимо подключиться.
     * @param desiredCapabilities задаёт какой браузер и с какими настройками требуется запускать.
     * @param recordingMode       задаёт режим фидеозаписи тестирования.
     */
    public JavisterWebDriverContainer(
            TestServiceContainer appContainer,
            DesiredCapabilities desiredCapabilities,
            VncRecordingMode recordingMode) {
        super();
        this.appContainer = appContainer;
        this.desiredCapabilities = desiredCapabilities;
        this.recordingMode = recordingMode;

        if (desiredCapabilities.getBrowserName().equals(BrowserType.CHROME)) {
            desiredCapabilities.setCapability("timeZone", getTimezone());
        }
    }

    /**
     * Создаёт контейнер, подключенный к заданному приложению и с заданными характеристиками.
     *
     * <p>В результате будет создан контейнер с браузером, указанным в @{code desiredCapabilities} и включенной записью
     * видео. Контейнер будет подключен к виртуальной сети переданного приложения.
     *
     * @param appContainer        обёртка над приложением, к которому необходимо подключиться.
     * @param desiredCapabilities задаёт какой браузер и с какими настройками требуется запускать.
     */
    public JavisterWebDriverContainer(
            TestServiceContainer appContainer,
            DesiredCapabilities desiredCapabilities) {

        this(appContainer, desiredCapabilities, VncRecordingMode.RECORD_ALL);
    }

    /**
     * Создаёт контейнер, подключенный к заданному приложению.
     *
     * <p>В результате будет создан контейнер с браузером Chrome и включенной записью
     * видео. Контейнер будет подключен к виртуальной сети переданного приложения.
     *
     * @param appContainer обёртка над приложением, к которому необходимо подключиться.
     */
    public JavisterWebDriverContainer(
            TestServiceContainer appContainer) {
        this(appContainer, DesiredCapabilities.chrome(), VncRecordingMode.RECORD_ALL);
    }

    /**
     * Создаёт контейнер WebDriver без привязки к приложению.
     *
     * <p>В результате будет создан контейнер с браузером Chrome и включенной записью
     * видео.
     *
     * <p>Для работы данного контейнера, его обязательно нужно связать с приложением, с которым он должен работать с помощью
     * метода {@link #withApplication(TestServiceContainer)}.
     */
    public JavisterWebDriverContainer() {
        this.recordingMode = VncRecordingMode.RECORD_ALL;
    }

    /**
     * Привязывает WebDriver в укзанному приложению.
     *
     * @param appContainer приложение, с которым должен работать WebDriver.
     * @return возвращает this для fluent API.
     */
    public JavisterWebDriverContainer withApplication(TestServiceContainer appContainer) {
        this.appContainer = appContainer;
        return this;
    }

    /**
     * Запустить браузер на локальном X сервере.
     * <p>Позволяет видеть процесс прохождения теста и смотреть состояние старницы при отладке.
     * <p>Запись видео будет форсированно выключена, т.к. она не совместима с переопределением адреса X сервера.
     * <p>Для Windows машин необходимо запустить
     * <a href="https://dev.to/darksmile92/run-gui-app-in-linux-docker-container-on-windows-host-4kde">локальный X сервер</a>.
     * <p>Для удалённо запускаемых контейнеров и для Windows необходимо явно указать переменную окружения DISPLAY с именем/IP вашей
     * локальной машины и номеров X экрана.
     *
     * @return возвращает this для fluent API.
     */
    public JavisterWebDriverContainer withLocalXServer() {
        this.recordingMode = BrowserWebDriverContainer.VncRecordingMode.SKIP;
        String display = System.getenv("DISPLAY");
        if (display == null || display.isEmpty()) {
            throw new IllegalStateException("Пременная окружения DISPLAY не установлена. Установите её явно для подключения браузера " +
                    "к вашему X серверу: https://dev.to/darksmile92/run-gui-app-in-linux-docker-container-on-windows-host-4kde");
        }
        if (System.getProperty("os.name").toLowerCase().contains("win") && display.charAt(0) == ':') {
            throw new IllegalStateException("Для Windows необходимо указать удалённое подключение к вашему X серверу в переменной " +
                    "окружения DISPLAY: https://dev.to/darksmile92/run-gui-app-in-linux-docker-container-on-windows-host-4kde");
        }
        if (display.charAt(0) == ':') {
            // Хакаем авторизацию X сервера, для возможности подключения из контейнера: https://habr.com/ru/post/240509/
            try {
                new ProcessExecutor().command(
                        "sh",
                        "-c",
                        "xauth nlist $DISPLAY | sed -e 's/^..../ffff/' | xauth -f /tmp/.docker.Xauthority nmerge -"
                ).redirectOutput(Slf4jStream.ofCaller().asInfo()).execute();
                withEnv("XAUTHORITY", "/tmp/.docker.Xauthority");
                withFileSystemBind("/tmp/.X11-unix", "/tmp/.X11-unix", BindMode.READ_WRITE);
                withFileSystemBind("/tmp/.docker.Xauthority", "/tmp/.docker.Xauthority", BindMode.READ_WRITE);
            } catch (IOException | TimeoutException e) {
                throw new TestRunException("Не удалось подготовить авторизацию X сервера для подключения из контейнера.", e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            withEnv("DISPLAY", "unix" + display);
            withEnv("START_XVFB", "false");
        }
        return this;
    }

    /**
     * /* Перехватываем создание контейнера, чтобы передать свои настройки.
     */
    @Override
    public void start() {
        if (desiredCapabilities != null) {
            this.withCapabilities(desiredCapabilities);
        }
        if (appContainer == null) {
            throw new IllegalStateException("Для контейнера KristaWebDriverContainer не установлен контейнер с приложением.");
        }
        if (USE_LOCAL_X_SERVER) {
            withLocalXServer();
        }
        this
                .withRecordingMode(recordingMode, appContainer.getTestPath())
                .withNetwork(appContainer.getNetwork());
        super.start();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!super.equals(o)) {
            return false;
        }
        JavisterWebDriverContainer that = (JavisterWebDriverContainer) o;
        return recordingMode == that.recordingMode
                && Objects.equals(desiredCapabilities, that.desiredCapabilities)
                && Objects.equals(appContainer, that.appContainer);
    }

    @Override
    public int hashCode() {

        return Objects.hash(super.hashCode(), appContainer, desiredCapabilities, recordingMode);
    }

    @Override
    public RemoteWebDriver getWebDriver() {
        RemoteWebDriver driver = super.getWebDriver();
        if (driver != null) {
            driver.manage().timeouts().implicitlyWait(100, TimeUnit.MILLISECONDS);
        }
        return driver;
    }

    private static String getTimezone() {
        return ZonedDateTime.now().getZone().getId();
    }

    public static JavisterWebDriverContainer byName(TestServiceContainer appContainer, String name) {
        return new JavisterWebDriverContainer(appContainer, Browser.valueOf(name.toUpperCase()).getCapabilities());
    }

    public enum Browser {
        CHROME(DesiredCapabilities.chrome()),
        FIREFOX(DesiredCapabilities.firefox());

        private DesiredCapabilities desiredCapabilities;

        Browser(DesiredCapabilities desiredCapabilities) {
            this.desiredCapabilities = desiredCapabilities;
        }

        public DesiredCapabilities getCapabilities() {
            return desiredCapabilities;
        }
    }
}
