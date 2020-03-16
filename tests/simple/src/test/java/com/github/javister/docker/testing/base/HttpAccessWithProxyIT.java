package com.github.javister.docker.testing.base;

import com.github.javister.docker.testing.TestContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.configuration.ConfigurationProperties;
import org.mockserver.mock.Expectation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MockServerContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;
import java.io.IOException;

import static org.mockserver.model.HttpRequest.request;

@Testcontainers
public class HttpAccessWithProxyIT {
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpAccessWithProxyIT.class);
    private static final Network externalNetwork = Network.newNetwork();
    private static final Network internalNetwork = Network.newNetwork();

    @Container
    @SuppressWarnings("unused")
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
            .withNetwork(externalNetwork)
            .waitingFor(Wait.forHttp("/").forPort(8080))
            .withLogConsumer(new Slf4jLogConsumer(LOGGER).withPrefix("mserver").withRemoveAnsiCodes(false));

    @Container
    private static final MockServerContainer proxyServer = new MockServerContainer()
            .dependsOn(mserver)
            .withNetworkAliases("proxy")
            .withNetwork(internalNetwork)
            .withExposedPorts(1080)
            .waitingFor(Wait.forHttp("/").forPort(1080).forStatusCode(404))
            .withLogConsumer(new Slf4jLogConsumer(LOGGER).withPrefix("proxy").withRemoveAnsiCodes(false));

    @Container
    private static final JavisterBaseContainer<?> container = new JavisterBaseContainer<>(SimpleImageTests.class)
            .withHttpProxy("http://proxy:1080")
            .withNoProxy("")
            .withImagePullPolicy(__ -> false)
            .withNetwork(internalNetwork)
            .dependsOn(proxyServer);

    @BeforeAll
    @SuppressWarnings("squid:S2095")
    public static void init() {
        proxyServer.getDockerClient().connectToNetworkCmd()
                .withContainerId(proxyServer.getContainerId())
                .withNetworkId(externalNetwork.getId())
                .exec();
    }

    @Test
    void testCurl() throws IOException, InterruptedException {
        MockServerClient proxyClient = getProxyClient();

        org.testcontainers.containers.Container.ExecResult exec = container.execInContainer(
                "curl",
                "-S",
                "-s",
                "http://mserver:8080/");
        Assertions.assertEquals(0, exec.getExitCode(), exec.getStderr());
        Assertions.assertEquals("Hello, world!", exec.getStdout().trim(), "Wrong response body");
//        Expectation[] expectations = proxyClient.retrieveRecordedExpectations(request("/"));
//        Assertions.assertEquals(1, expectations.length, exec.getStderr());
//        expectations[0].getHttpResponse().withBody("Hello, world!");
//        proxyClient.clear(request("/"));
    }

    @Test
    void testWget() throws IOException, InterruptedException {
        MockServerClient proxyClient = getProxyClient();

        org.testcontainers.containers.Container.ExecResult exec = container.execInContainer(
                "wget",
                "-q",
                "-O",
                "-",
                "http://mserver:8080/");
        Assertions.assertEquals(0, exec.getExitCode(), exec.getStderr());
        Assertions.assertEquals("Hello, world!", exec.getStdout().trim(), "Wrong response body");
//        Expectation[] expectations = proxyClient.retrieveRecordedExpectations(request("/"));
//        Assertions.assertEquals(1, expectations.length, exec.getStderr());
//        expectations[0].getHttpResponse().withBody("Hello, world!");
//        proxyClient.clear(request("/"));
    }

    private static MockServerClient getProxyClient() {
        return new MockServerClient(proxyServer.getContainerIpAddress(), proxyServer.getMappedPort(1080));
    }
}
