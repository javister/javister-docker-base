package com.github.javister.docker.testing.base;

import com.github.javister.docker.testing.JavisterWebDriverContainer;
import com.github.javister.docker.testing.TestContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;

@Testcontainers
public class JavisterWebDriverIT {
    private static final Network network = Network.newNetwork();

    @Container
    private static final JavisterBaseContainer<?> mserver = new JavisterBaseContainerImpl<>(
            JavisterWebDriverIT.class,
            new ImageFromDockerfile()
                    .withFileFromFile(
                            "app.jar",
                            new File(JavisterBaseContainer.getTestPath(JavisterWebDriverIT.class) + "/test-app.jar")
                    )
                    .withDockerfileFromBuilder(builder ->
                            builder
                                    .from("bellsoft/liberica-openjre-alpine:8")
                                    .add("app.jar", "/app.jar")
                                    .cmd("java", "-jar", "/app.jar")
                                    .expose(8080)
                                    .build()
                    )
    )
            .withExposedPorts(8080)
            .withNetworkAliases("mserver")
            .withNetwork(network)
            .waitingFor(Wait.forHttp("/").forPort(8080));

    @ParameterizedTest
    @EnumSource(JavisterWebDriverContainer.Browser.class)
    void testCustomWebDriverContainer(JavisterWebDriverContainer.Browser browserType, TestInfo testInfo) throws Throwable {
        TestContext.runTestIn(mserver, testInfo, browserType, 2000L, context -> {
            // Получаем WebDriver, настроенный на запущенный контейнер с приложением
            WebDriver driver = context.getDriver();
            driver.get("http://mserver:8080/");
            Assertions.assertEquals("Hello, world!", driver.findElement(By.tagName("body")).getText(), "Wrong response body");
        });
    }
}
