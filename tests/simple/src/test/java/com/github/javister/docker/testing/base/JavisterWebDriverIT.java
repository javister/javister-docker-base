package com.github.javister.docker.testing.base;

import com.github.javister.docker.testing.selenium.JavisterWebDriverConfigurator;
import com.github.javister.docker.testing.selenium.JavisterWebDriverContainer;
import com.github.javister.docker.testing.selenium.JavisterWebDriverContainer.Browser;
import com.github.javister.docker.testing.selenium.JavisterWebDriverProvider;
import io.qameta.allure.Description;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.openqa.selenium.remote.BrowserType;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class JavisterWebDriverIT extends JavisterWebDriverBase {

    @ParameterizedTest
    @EnumSource(Browser.class)
    @Description("WebDriver: перебор вариантов браузеров с ручной настройкой и запуском")
    void testCustomWebDriverContainer(Browser browserType) {
        try (JavisterWebDriverContainer webContainer = new JavisterWebDriverContainer(mserver, browserType.getCapabilities())
                .withImplicitlyWait(2000L)) {
            webContainer.start();

            RemoteWebDriver driver = webContainer.getWebDriver();
            simpleTest(driver);
        }
    }

    @JavisterWebDriverProvider(autostart = false)
    @Description("WebDriver: JavisterWebDriverProvider без автостарта")
    void testCustomWebDriverContainer(JavisterWebDriverContainer webContainer) {
        try {
            webContainer
                    .withImplicitlyWait(2000L)
                    .withApplication(mserver)
                    .start();

            simpleTest(webContainer.getWebDriver());
        } finally {
            webContainer.close();
        }
    }

    @JavisterWebDriverProvider(configuratorClass = TestConfigurator.class, autostart = false)
    @Description("WebDriver: JavisterWebDriverProvider без автостарта и с кастомным провайдером настройки контейнера")
    void testCustomWebDriverContainerWithConfigurator(JavisterWebDriverContainer webContainer) {
        try {
            webContainer.start();
            simpleTest(webContainer.getWebDriver());
        } finally {
            webContainer.close();
        }
    }

    @JavisterWebDriverProvider(value = Browser.CHROME, autostart = false)
    @Description("WebDriver: JavisterWebDriverProvider без автостарта и только Chrome")
    void testCustomWebDriverContainerChrome(JavisterWebDriverContainer webContainer) {
        Assertions.assertNotNull(webContainer.getDesiredCapabilities(), "Свойства браузера должны быть установлены.");
        Assertions.assertEquals(
                BrowserType.CHROME,
                webContainer.getDesiredCapabilities().getBrowserName(),
                "Должен быть выбран браузер Chrome"
        );
    }

    @JavisterWebDriverProvider(value = Browser.FIREFOX, autostart = false)
    @Description("WebDriver: JavisterWebDriverProvider без автостарта и только Firefox")
    void testCustomWebDriverContainerFirefox(JavisterWebDriverContainer webContainer) {
        Assertions.assertNotNull(webContainer.getDesiredCapabilities(), "Свойства браузера должны быть установлены.");
        Assertions.assertEquals(
                BrowserType.FIREFOX,
                webContainer.getDesiredCapabilities().getBrowserName(),
                "Должен быть выбран браузер Firefox"
        );
    }

    @JavisterWebDriverConfigurator
    void configuratorMember(Browser type, JavisterWebDriverContainer webContainer, ParameterContext paramContext, ExtensionContext extContext) {
        Assertions.assertNotNull(type, "Тип браузера не должен быть null");
        Assertions.assertNotNull(webContainer, "Контейнер не должен быть null");
        Assertions.assertNotNull(paramContext, "Котекст параметра не должен быть null");
        Assertions.assertNotNull(extContext, "Котекст расширения не должен быть null");
    }

    @JavisterWebDriverConfigurator
    static void configuratorStatic(Browser type, JavisterWebDriverContainer webContainer, ParameterContext paramContext, ExtensionContext extContext) {
        Assertions.assertNotNull(type, "Тип браузера не должен быть null");
        Assertions.assertNotNull(webContainer, "Контейнер не должен быть null");
        Assertions.assertNotNull(paramContext, "Котекст параметра не должен быть null");
        Assertions.assertNotNull(extContext, "Котекст расширения не должен быть null");
    }

    public static class TestConfigurator implements JavisterWebDriverProvider.Configurator {
        @Override
        public void configure(JavisterWebDriverContainer webContainer, Browser type) {
            Assertions.assertNotNull(type, "Тип браузера не должен быть null");
            Assertions.assertNotNull(webContainer, "Контейнер не должен быть null");
            webContainer
                    .withImplicitlyWait(2000L)
                    .withApplication(mserver);
        }
    }
}
