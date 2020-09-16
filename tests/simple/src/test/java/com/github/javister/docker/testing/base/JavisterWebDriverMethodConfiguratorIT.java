package com.github.javister.docker.testing.base;

import com.github.javister.docker.testing.selenium.JavisterWebDriverContainer;
import com.github.javister.docker.testing.selenium.JavisterWebDriverConfigurator;
import com.github.javister.docker.testing.selenium.JavisterWebDriverProvider;
import io.qameta.allure.Description;
import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public class JavisterWebDriverMethodConfiguratorIT extends JavisterWebDriverBase {

    @JavisterWebDriverProvider(autostart = false)
    @Description("WebDriverMethodConfigurator: запуск контейнера без автостарта и инжекция как JavisterWebDriverContainer")
    void testCustomWebDriverContainer(JavisterWebDriverContainer webContainer) {
        Assertions.assertFalse(webContainer.isRunning(), "Контейнер должен быть не запущен");

        try {
            webContainer.start();
            RemoteWebDriver driver = webContainer.getWebDriver();
            simpleTest(driver);
        } finally {
            webContainer.close();
        }
    }

    @JavisterWebDriverProvider
    @Description("WebDriverMethodConfigurator: запуск контейнера с автостартом и инжекция как JavisterWebDriverContainer")
    void testCustomWebDriverContainerAutostart(JavisterWebDriverContainer webContainer) {
        Assertions.assertTrue(webContainer.isRunning(), "Контейнер должен быть запущен");

        RemoteWebDriver driver = webContainer.getWebDriver();
        simpleTest(driver);
    }

    @JavisterWebDriverProvider(autostart = false)
    @Description("WebDriverMethodConfigurator: запуск контейнера без автостарта и инжекция как RemoteWebDriver")
    void testCustomRemoteWebDriver(RemoteWebDriver driver) {
        simpleTest(driver);
    }

    @JavisterWebDriverProvider
    @Description("WebDriverMethodConfigurator: запуск контейнера с автостартом и инжекция как RemoteWebDriver")
    void testCustomRemoteWebDriverAutostart(RemoteWebDriver driver) {
        simpleTest(driver);
    }

    @JavisterWebDriverProvider(autostart = false)
    @Description("WebDriverMethodConfigurator: запуск контейнера без автостарта и инжекция как WebDriver")
    void testCustomWebDriver(WebDriver driver) {
        simpleTest(driver);
    }

    @JavisterWebDriverProvider
    @Description("WebDriverMethodConfigurator: запуск контейнера с автостартом и инжекция как WebDriver")
    void testCustomWebDriverAutostart(WebDriver driver) {
        simpleTest(driver);
    }

    @JavisterWebDriverConfigurator
    static void configuratorStatic(JavisterWebDriverContainer webContainer) {
        webContainer
                .withImplicitlyWait(2000L)
                .withApplication(mserver);
    }
}
