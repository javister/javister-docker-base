package com.github.javister.docker.testing.base;

import com.github.javister.docker.testing.selenium.JavisterWebDriverContainer;
import com.github.javister.docker.testing.selenium.JavisterWebDriverConfigurator;
import com.github.javister.docker.testing.selenium.JavisterWebDriverProvider;
import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public class JavisterWebDriverMethodConfiguratorIT extends JavisterWebDriverBase {

    @JavisterWebDriverProvider(autostart = false)
    void testCustomWebDriverContainer(JavisterWebDriverContainer webContainer) {
        Assertions.assertFalse(webContainer.isRunning(), "Сонтейнер должен быть незапущен");

        webContainer.start();
        RemoteWebDriver driver = webContainer.getWebDriver();
        simpleTest(driver);
    }

    @JavisterWebDriverProvider
    void testCustomWebDriverContainerAutostart(JavisterWebDriverContainer webContainer) {
        Assertions.assertTrue(webContainer.isRunning(), "Сонтейнер должен быть запущен");

        RemoteWebDriver driver = webContainer.getWebDriver();
        simpleTest(driver);
    }

    @JavisterWebDriverProvider(autostart = false)
    void testCustomRemoteWebDriver(RemoteWebDriver driver) {
        simpleTest(driver);
    }

    @JavisterWebDriverProvider
    void testCustomRemoteWebDriverAutostart(RemoteWebDriver driver) {
        simpleTest(driver);
    }

    @JavisterWebDriverProvider(autostart = false)
    void testCustomWebDriver(WebDriver driver) {
        simpleTest(driver);
    }

    @JavisterWebDriverProvider
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
