package com.github.javister.docker.testing.base;

import com.github.dockerjava.api.command.InspectContainerResponse;
import io.qameta.allure.Description;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockserver.client.MockServerClient;
import org.mockserver.mock.Expectation;
import org.mockserver.model.HttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Container.ExecResult;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static org.mockserver.model.HttpRequest.request;

@Testcontainers
public class HttpAccessWithProxyWithAuthIT {
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpAccessWithProxyWithAuthIT.class);
    private static final Network externalNetwork = Network.newNetwork();
    private static final Network internalNetwork = Network.newNetwork();

    private static final Map<String, String> ENV_MAP = new HashMap<>();

    static {
        ENV_MAP.put("http_proxy", "http://system:masterkey@proxy:1080");
        ENV_MAP.put("https_proxy", "https://system:masterkey@proxy:1080");
        ENV_MAP.put("PROXY", "http://system:masterkey@proxy:1080");
        ENV_MAP.put("PROXY_USER", "system");
        ENV_MAP.put("PROXY_PASS", "masterkey");
        ENV_MAP.put("PROXY_HOST", "proxy");
        ENV_MAP.put("PROXY_PORT", "1080");
    }

    @Container
    @SuppressWarnings("unused")
    private static final GenericContainer<?> mserver = new GenericContainer<>(
            new ImageFromDockerfile()
                    .withFileFromFile(
                            "app.jar",
                            new File(JavisterBaseContainer.getTestPath(HttpAccessWithProxyWithAuthIT.class) + "/test-app.jar")
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
    private static final MockServerContainer proxyServer = new MockServerContainer("5.11.1") {
        // Workaround for https://github.com/moby/moby/issues/40740
        @Override
        protected void containerIsStarting(InspectContainerResponse containerInfo) {
            getDockerClient().connectToNetworkCmd()
                    .withContainerId(getContainerId())
                    .withNetworkId(externalNetwork.getId())
                    .exec();
            super.containerIsStarting(containerInfo);
        }

        @Override
        public InspectContainerResponse getContainerInfo() {
            return getCurrentContainerInfo();
        }
    }
            .dependsOn(mserver)
            .withNetworkAliases("proxy")
            .withNetwork(internalNetwork)
            .withExposedPorts(1080)
            .waitingFor(Wait.forHttp("/").forPort(1080).forStatusCode(404))
            .withLogConsumer(new Slf4jLogConsumer(LOGGER).withPrefix("proxy").withRemoveAnsiCodes(false));

    @Container
    private static final JavisterBaseContainer<?> container = new JavisterBaseContainerImpl<>(HttpAccessWithProxyWithAuthIT.class)
            .withHttpProxy("http://system:masterkey@proxy:1080")
            .withNoProxy("")
            .withImagePullPolicy(__ -> false)
            .withNetwork(internalNetwork)
            .dependsOn(proxyServer);

    @Test
    @Description("Тест работы HTTP Proxy из контейнера через curl с аутентификацией")
    void testCurl() throws IOException, InterruptedException {
        ExecResult exec = container.execInContainer(
                "curl",
                "-S",
                "-s",
                "http://mserver:8080/");
        Assertions.assertEquals(0, exec.getExitCode(), exec.getStderr());
        Assertions.assertEquals("Hello, world!", exec.getStdout().trim(), "Wrong response body");

        MockServerClient proxyClient = getProxyClient();
        Expectation[] expectations = proxyClient.retrieveRecordedExpectations(request("/"));
        Assertions.assertEquals(1, expectations.length, exec.getStderr());
        Assertions.assertEquals("Hello, world!", expectations[0].getHttpResponse().getBodyAsString());
        Assertions.assertTrue(((HttpRequest) expectations[0].getHttpRequest()).containsHeader("Proxy-Authorization"), "Авторизация на прокси должна присутствовать");
        proxyClient.clear(request("/"));
    }

    @Test
    @Description("Тест работы HTTP Proxy из контейнера через wget с аутентификацией")
    void testWget() throws IOException, InterruptedException {
        ExecResult exec = container.execInContainer(
                "wget",
                "-q",
                "-O",
                "-",
                "http://mserver:8080/");
        Assertions.assertEquals(0, exec.getExitCode(), exec.getStderr());
        Assertions.assertEquals("Hello, world!", exec.getStdout().trim(), "Wrong response body");

        MockServerClient proxyClient = getProxyClient();
        Expectation[] expectations = proxyClient.retrieveRecordedExpectations(request("/"));
        Assertions.assertEquals(1, expectations.length, exec.getStderr());
        Assertions.assertEquals("Hello, world!", expectations[0].getHttpResponse().getBodyAsString());
        Assertions.assertTrue(((HttpRequest) expectations[0].getHttpRequest()).containsHeader("Proxy-Authorization"), "Авторизация на прокси должна присутствовать");
        proxyClient.clear(request("/"));
    }

    @ParameterizedTest(name = "{0}={1}")
    @MethodSource("envParams")
    @Description("Тест настройки HTTP Proxy с аутентификацией")
    void testProxyenvs(String envName, String envVal) throws IOException, InterruptedException {
        ExecResult exec = container.execInContainer(
                "bash",
                "-c",
                ". proxyenv; echo $" + envName);
        Assertions.assertEquals(0, exec.getExitCode(), exec.getStderr());
        Assertions.assertEquals(envVal, exec.getStdout().trim(), "Wrong env value");
    }

    private static Stream<Arguments> envParams() {
        ArrayList<Arguments> args = new ArrayList<>();
        ENV_MAP.forEach((env, val) -> args.add(Arguments.of(env, val)));
        return args.stream();
    }

    private static MockServerClient getProxyClient() {
        return new MockServerClient(proxyServer.getContainerIpAddress(), proxyServer.getMappedPort(1080));
    }
}
