package com.github.javister.docker.testing.base;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.time.Duration;

import static java.time.temporal.ChronoUnit.SECONDS;

@Testcontainers
public class UserTests {
    @Container
    @SuppressWarnings({"squid:S1905", "squid:S00117"})
    private static final JavisterBaseContainer<?> container = new JavisterBaseContainerImpl<>(UserTests.class)
            .withUserName("user1")
            .withEnv("LOG_LEVEL", "DEBUG")
            .waitingFor(new LogMessageWaitStrategy()
                    .withRegEx(".*syslog-ng starting up.*\\s")
                    .withTimes(1)
                    .withStartupTimeout(Duration.of(60, SECONDS))
            )
            .withImagePullPolicy(__ -> false);

    @Test
    void testRootPrivileges() throws IOException, InterruptedException {
        ExecResult result;
        result = container.execInContainer("setuser", "user1", "whoami");
        Assertions.assertEquals(0, result.getExitCode(), result.getStderr());
        Assertions.assertEquals("user1", result.getStdout().trim(), "Должно быть установлено новое имя пользователя");
    }
}
