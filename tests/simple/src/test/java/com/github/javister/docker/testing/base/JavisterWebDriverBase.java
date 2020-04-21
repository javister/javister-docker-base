package com.github.javister.docker.testing.base;

import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.junit.jupiter.Container;

import java.io.File;

public class JavisterWebDriverBase {
    static final Network network = Network.newNetwork();
    @Container
    static final JavisterBaseContainer<?> mserver = new JavisterBaseContainerImpl<>(
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
}
