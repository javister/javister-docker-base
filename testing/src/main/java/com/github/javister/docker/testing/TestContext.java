package com.github.javister.docker.testing;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.TestInfo;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testcontainers.containers.BrowserWebDriverContainer;
import org.testcontainers.lifecycle.TestDescription;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.junit.Assert.fail;

/**
 * Вспомогательный класс, контекст, для упрощения написания тестов на Selenium WebDriver.
 */
public class TestContext {

    private final Class<?> testClass;
    private final BrowserWebDriverContainer browser;
    private final long implicitlyWait;
    private WebDriver driver;
    private WebDriverWait wait;

    /**
     * Создаёт объект класса для конкретного теста и браузера.
     *
     * @param testClass      класс теста для которого создается контекст.
     * @param browser        обёртка над браузером, через который будет работать WebDriver.
     * @param implicitlyWait глобальная настройка таймаута при ожиданиях какого либо события.
     */
    public TestContext(Class<?> testClass, BrowserWebDriverContainer browser, long implicitlyWait) {
        this.testClass = testClass;
        this.browser = browser;
        this.implicitlyWait = implicitlyWait;
        init();
    }

    /**
     * Возвращает обёртку над браузером, через который работает WebDriver.
     *
     * @return возвращает обёртку над браузером, через который работает WebDriver.
     */
    public BrowserWebDriverContainer getBrowser() {
        return browser;
    }

    /**
     * Возвращает WebDriver для работы с браузером.
     *
     * @return WebDriver для работы с браузером.
     */
    public WebDriver getDriver() {
        return driver;
    }

    /**
     * Ожидает  появления элемента, заданного селектором.
     *
     * @param by селектор элемента, появления которого необходимо дождаться.
     */
    public void waitForElement(By by) {
        wait.until(ExpectedConditions.presenceOfElementLocated(by));
    }

    /**
     * Проверяет факт наличия элемента, заданного селектором, в DOM структуре WEB страницы.
     *
     * @param by селектор элемента, наличие которого необходимо проверить.
     * @return возвращает true, если элемент присутствует в DOM структуре.
     */
    public boolean isElementPresent(By by) {
        driver.manage().timeouts().implicitlyWait(100, TimeUnit.MILLISECONDS);
        try {
            return !driver.findElements(by).isEmpty();
        } finally {
            driver.manage().timeouts().implicitlyWait(implicitlyWait, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Дожидается элемента в DOM структуре WEB страницы и генерирует событие клика левой кнопки мыши на нём.
     *
     * <p>В общем случае элемент может присутствовать в DOM структуре, но быть перекрытым другими элементами.
     * В таком случае попытка сделать клик на нём приведёт в исключению на уровне WebDriver. Данный метод
     * обрабатывает такую ситуацию и не просто дожидается появления элемента но и пытается повторять попытки клика
     * по элементу до тех пор, пока тот не станет доступен для такой операции. Ну или пока не закончится таймаут.
     *
     * <p>Таймаут, задачаемый параметром {@code timeout} довольно условный. В силу некоторых особенностей работы
     * WebDriver он может оказываться значительно больше. Особенно в случаях, когда элемент долго не появляется в
     * DOM структуре, тогда могут примешиваться доплнительные таймауты на уровне WebDriver.
     *
     * <p>squid:S2221 - в процессе ожидания может возникать большое количество всевозможных ошибок, который довольно
     * проблематично угадать заранее.
     *
     * <p>squid:S1166 - в данном конкретном случае нормально, что генерятся ошибки. Логгироать их не имеет никакого
     * смысла.
     *
     * <p>pmd:AvoidBranchingStatementAsLastInLoop - можно переписать иначе, но тогда алгоритм будет более
     * громоздкий.
     *
     * @param selector селектор элемента в DOM структуре над которым необходимо на который необходимо кликнуть.
     * @param timeout  таймаут в секундах в течении которого необходимо продолжать попытки кликнуть на элемент.
     */
    @SuppressWarnings({"squid:S2221", "squid:S1166", "pmd:AvoidBranchingStatementAsLastInLoop"})
    public void waitAndClick(By selector, int timeout) {
        for (int second = 0; ; second++) {
            if (second >= timeout) {
                fail("timeout");
            }
            try {
                driver.findElement(selector).click();
            } catch (Exception e) {
                sleep(1000);
                continue;
            }
            break;
        }
    }

    /**
     * Ожидание готовности сервера по заданному адресу и селектору элемента на странице.
     *
     * <p>Т.к. старт окружения может занимать довольно большое время, то может возникать необходимость дождаться
     * готовности приложения принимать HTTP запросы. Для этого производятся периодические попытки открыть
     * странизу по заданному адресу и наёти там указанный DOM элемент.
     * <p>
     * Если страница открылась и на ней присутствует указанный элемент, то считается, что сервер готов к работе.
     *
     * <p>squid:S2221 - в процессе ожидания может возникать большое количество всевозможных ошибок, который довольно
     * проблематично угадать заранее.
     *
     * <p>squid:S1166 - в данном конкретном случае нормально, что генерятся ошибки. Логгироать их не имеет никакого
     * смысла.
     *
     * @param address  HTTP адрес страницы на сервере.
     * @param selector селектор элемента в DOM структуре, который должен присутствовать на странице.
     */
    @SuppressWarnings({"squid:S2221", "squid:S1166"})
    public void waitForServerReady(String address, By selector) {
        for (int second = 0; ; second++) {
            if (second >= implicitlyWait) {
                fail("timeout");
            }
            try {
                driver.get(address);
                wait
                        .withTimeout(Duration.of(1, ChronoUnit.SECONDS))
                        .until(ExpectedConditions.presenceOfElementLocated(selector));
                break;
            } catch (Exception e) {
                // подавляем все исключения в ожидании нормализации ситуации.
            }
            sleep(1000);
        }
    }

    /**
     * Метод, запускающий тест в рамках текущего контекста.
     *
     * <p>Данный метод необходим для корректного управлением запуском и остановкой контейнеров с WebDriver в
     * начале и конце выполнения теста.
     *
     * <p>Необходимость в этом может возникнуть в случае, когда есть необходимость запускать один и от же тест
     * с разными конфигурациями браузеров. Тогда сам тест можно вынести в отдельный метод, принимающий объект
     * класса {@code TestContext}, а за тем в разных тестовых методах создавать WebDriver с разными настройками
     * и вызывать основной метод с тестом через данный метод.
     *
     * <p>Пример:
     * <pre>
     *    {@literal @}Test
     *     public void testRegisterUsersOnChrome() throws Throwable {
     *         BrowserWebDriverContainer browser = new KristaWebDriverContainer(application);
     *         TestContext context = new TestContext(WildflyHibernate4Test.class, browser, 120);
     *         context.runTestIn("testRegisterUsersOnChrome", this::registrationCommands);
     *     }
     *
     *     private void registrationCommands(TestContext context) {
     *         WebDriver driver = context.getDriver();
     *         context.waitForServerReady("http://application:8080/wildfly-hibernate4/index.jsf", By.id("reg:id"));
     *
     *         ...
     *     }
     * </pre>
     *
     * <p>squid:S00112 - данная сигнатура диктуется со стороны API у JUnit.
     *
     * @param testName имя теста, которе будет отображаться в отчётах JUnit.
     * @param test     метод с основной логикой теста.
     * @throws Throwable любые исключения будут выброшены наружу для корректной обработки на уровне JUnit.
     */
    @SuppressWarnings("squid:S00112")
    public void runTestIn(String testName, Consumer<TestContext> test) throws Throwable {
        browser.apply(new Statement() {
            @Override
            public void evaluate() {
                init();
                test.accept(TestContext.this);
            }
        }, Description.createTestDescription(testClass, testName)).evaluate();
    }

    /**
     * Утилитный метод для организации ожиданий.
     *
     * @param millis величина задержки в милисекундах.
     */
    public static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            throw new TestRunException("Can't do wait", e);
        }
    }

    public static void runTestIn(
            TestServiceContainer application, TestInfo testInfo,
            JavisterWebDriverContainer.Browser browserType,
            long implicitlyWait,
            Consumer<TestContext> test) {
        try (JavisterWebDriverContainer container = new JavisterWebDriverContainer(application, browserType.getCapabilities())) {
            Throwable error = null;
            try {
                Class<?> testClass = testInfo.getTestClass().orElse(TestContext.class);
                TestContext context = new TestContext(testClass, container, implicitlyWait);
                container.start();
                context.init();
                test.accept(context);
            } catch (Throwable t) {
                error = t;
            } finally {
                TestDescription description = getDescription(testInfo);
                container.afterTest(description, Optional.ofNullable(error));
            }
        }
    }

    private void init() {
        WebDriver webDriver = browser.getWebDriver();
        if (webDriver != null) {
            this.driver = webDriver;
            this.driver.manage().timeouts().implicitlyWait(implicitlyWait, TimeUnit.MILLISECONDS);
            this.wait = new WebDriverWait(webDriver, implicitlyWait);
        }
    }

    @NotNull
    private static TestDescription getDescription(TestInfo testInfo) {
        String testMethod = testInfo.getTestMethod().map(it -> it.getName() + "-").orElse("");
        Description description = Description.createTestDescription(
                testInfo.getTestClass().orElse(TestContext.class),
                testMethod + testInfo.getDisplayName());
        return new TestDescription() {
            @Override
            public String getTestId() {
                return description.getDisplayName();
            }

            @Override
            public String getFilesystemFriendlyName() {
                return description.getClassName() + "-" + description.getMethodName();
            }
        };
    }
}
