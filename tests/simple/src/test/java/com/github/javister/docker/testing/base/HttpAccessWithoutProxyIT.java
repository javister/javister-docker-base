package com.github.javister.docker.testing.base;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;
import java.io.IOException;

@Testcontainers
public class HttpAccessWithoutProxyIT {
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpAccessWithoutProxyIT.class);
    private static final Network network = Network.newNetwork();

    @Container
    private static final GenericContainer<?> mserver = new GenericContainer<>(
            new ImageFromDockerfile()
                    .withFileFromFile(
                            "app.jar",
                            new File(JavisterBaseContainer.getTestPath(SimpleImageTests.class) + "/test-app.jar")
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
            .waitingFor(Wait.forHttp("/").forPort(8080))
            .withLogConsumer(new Slf4jLogConsumer(LOGGER).withPrefix("mserver").withRemoveAnsiCodes(false));

    @Container
    private static final JavisterBaseContainer<?> container = new JavisterBaseContainer<>(SimpleImageTests.class)
            .autoHttpProxy(false)
            .withImagePullPolicy(__ -> false)
            .withNetwork(network);

    @Test
    void testCurl() throws IOException, InterruptedException {
        ExecResult exec = container.execInContainer(
                "curl",
                "-S",
                "-s",
                "http://mserver:8080/");
        Assertions.assertEquals(0, exec.getExitCode(), exec.getStderr());
        Assertions.assertEquals("Hello, world!", exec.getStdout().trim(), "Wrong response body");
    }

    @Test
    void testWget() throws IOException, InterruptedException {
        ExecResult exec = container.execInContainer(
                "wget",
                "-q",
                "-O",
                "-",
                "http://mserver:8080/");
        Assertions.assertEquals(0, exec.getExitCode(), exec.getStderr());
        Assertions.assertEquals("Hello, world!", exec.getStdout().trim(), "Wrong response body");
    }
}
