package com.github.javister.docker.testing.base;

import com.github.javister.docker.testing.selenium.JavisterWebDriverConfigurator;
import com.github.javister.docker.testing.selenium.JavisterWebDriverContainer;
import com.github.javister.docker.testing.selenium.JavisterWebDriverContainer.Browser;
import com.github.javister.docker.testing.selenium.JavisterWebDriverProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.openqa.selenium.By;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.lifecycle.TestDescription;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class JavisterWebDriverVideoIT extends JavisterWebDriverBase {

    @JavisterWebDriverConfigurator
    @SuppressWarnings({"unused", "RedundantSuppression"})
    void webDriverConfig(JavisterWebDriverContainer webContainer) {
        webContainer
                .withImplicitlyWait(200L)
                .withApplication(mserver);
    }

    @BeforeAll
    static void cleanBeforeAll() throws IOException {
        getVideoFiles("testVideoRecordingTemplate").forEach(path -> {
            try {
                Files.delete(path);
            } catch (IOException e) {
                throw new RuntimeException("Ошибка удаления файла: " + path.toString(), e);
            }
        });
    }

    @Order(1)
    @JavisterWebDriverProvider()
    void testVideoRecordingTemplateSuccess(JavisterWebDriverContainer webContainer) {
        simpleTest(webContainer.getWebDriver());
    }

    @Order(2)
    @JavisterWebDriverProvider(autostart = false)
    void testVideoRecordingTemplateFail(JavisterWebDriverContainer webContainer) {
        String name = "testVideoRecordingTemplateFail";
        webContainer
                .withImplicitlyWait(200L)
                .withApplication(mserver)
                .start();

        RemoteWebDriver driver = webContainer.getWebDriver();
        driver.get("http://mserver:8080/");
        Throwable throwable = Assertions.assertThrows(Throwable.class, () -> driver.findElement(By.id("zzz")).click());
        webContainer.afterTest(getDescription(name, webContainer.getDesiredCapabilities().getBrowserName().toUpperCase()), Optional.of(throwable));
        webContainer.close();
    }

    @Test
    @Order(3)
    void testVideoRecordingTemplateFiles() throws IOException {
        checkFilesSuccess("testVideoRecordingTemplateSuccess");
        checkFilesFailed("testVideoRecordingTemplateFail");

    }

    @Test
    @Order(100)
    void testVideoRecordingTestSuccess() throws IOException {
        String name = "testVideoRecordingTestSuccess";
        getVideoFiles(name).forEach(path -> {
            try {
                Files.delete(path);
            } catch (IOException e) {
                throw new RuntimeException("Ошибка удаления файла: " + path.toString(), e);
            }
        });

        try (JavisterWebDriverContainer webContainer = new JavisterWebDriverContainer(mserver, Browser.CHROME.getCapabilities())
                .withImplicitlyWait(2000L)) {
            webContainer.start();
            RemoteWebDriver driver = webContainer.getWebDriver();
            simpleTest(driver);
            webContainer.afterTest(getDescription(name, Browser.CHROME.name()), Optional.empty());
        }
        try (JavisterWebDriverContainer webContainer = new JavisterWebDriverContainer(mserver, Browser.FIREFOX.getCapabilities())
                .withImplicitlyWait(2000L)) {
            webContainer.start();
            RemoteWebDriver driver = webContainer.getWebDriver();
            simpleTest(driver);
            webContainer.afterTest(getDescription(name, Browser.FIREFOX.name()), Optional.empty());
        }

        checkFilesSuccess(name);
    }

    @Test
    @Order(100)
    void testVideoRecordingTestFailed() throws IOException {
        String name = "testVideoRecordingTestFailed";
        getVideoFiles(name).forEach(path -> {
            try {
                Files.delete(path);
            } catch (IOException e) {
                throw new RuntimeException("Ошибка удаления файла: " + path.toString(), e);
            }
        });

        try (JavisterWebDriverContainer webContainer = new JavisterWebDriverContainer(mserver, Browser.CHROME.getCapabilities())
                .withImplicitlyWait(2000L)) {
            webContainer.start();
            RemoteWebDriver driver = webContainer.getWebDriver();
            simpleTest(driver);
            webContainer.afterTest(getDescription(name, Browser.CHROME.name()), Optional.of(new RuntimeException()));
        }
        try (JavisterWebDriverContainer webContainer = new JavisterWebDriverContainer(mserver, Browser.FIREFOX.getCapabilities())
                .withImplicitlyWait(2000L)) {
            webContainer.start();
            RemoteWebDriver driver = webContainer.getWebDriver();
            simpleTest(driver);
            webContainer.afterTest(getDescription(name, Browser.FIREFOX.name()), Optional.of(new RuntimeException()));
        }

        checkFilesFailed(name);
    }

    private void checkFilesFailed(String name) throws IOException {
        List<Path> files = getVideoFiles(name).stream()
                .filter(f -> f.toString().contains("FAILED"))
                .collect(Collectors.toList());
        Assertions.assertEquals(2, files.size(), "Должно было создаться два видео файла с проваленными тестами");
        Assertions.assertEquals(
                1,
                files.stream().filter(f -> f.toString().contains(Browser.CHROME.name())).count(),
                "Должна была создаться видеозапись Chrome с именем, включающим: " + name);
        Assertions.assertEquals(
                1,
                files.stream().filter(f -> f.toString().contains(Browser.FIREFOX.name())).count(),
                "Должна была создаться видеозапись Firefox с именем, включающим: " + name);
    }

    private void checkFilesSuccess(String name) throws IOException {
        List<Path> files = getVideoFiles(name).stream()
                .filter(f -> f.toString().contains("PASSED"))
                .collect(Collectors.toList());
        Assertions.assertEquals(2, files.size(), "Должно было создаться два видео файла с успешными тестами");
        Assertions.assertEquals(
                1,
                files.stream().filter(f -> f.toString().contains(Browser.CHROME.name())).count(),
                "Должна была создаться видеозапись Chrome с именем, включающим: " + name);
        Assertions.assertEquals(
                1,
                files.stream().filter(f -> f.toString().contains(Browser.FIREFOX.name())).count(),
                "Должна была создаться видеозапись Firefox с именем, включающим: " + name);
    }

    private static List<Path> getVideoFiles(String name) throws IOException {
        return Files.list(JavisterBaseContainer.getTestPath(JavisterWebDriverVideoIT.class).toPath())
                .filter(file -> file.toString().endsWith(".flv"))
                .filter(file -> file.toString().contains(name))
                .collect(Collectors.toList());
    }

    private TestDescription getDescription(String name, String browserType) {
        return new TestDescription() {
            @Override
            public String getTestId() {
                return name;
            }

            @Override
            public String getFilesystemFriendlyName() {
                return name + "-" + browserType;
            }
        };
    }
}
