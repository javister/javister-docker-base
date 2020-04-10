package com.github.javister.docker.testing.base;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
public class SimpleImageTests {
    @Container
    @SuppressWarnings({"squid:S1905", "squid:S00117"})
    private static final JavisterBaseContainer<?> container = new JavisterBaseContainerImpl<>(SimpleImageTests.class)
            .withRelativeFileSystemBind(".", "/app")
            .withImagePullPolicy(__ -> false);

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4})
    void waitForPort(int delay) {
        assertFalse(assertTimeout(
                Duration.ofSeconds(delay + 1L),
                () -> container.waitConnectionOpen("localhost", 1111, delay)),
                "Слишком короткое ожидание");
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    void testRootPrivileges() throws IOException, InterruptedException {
        String template = "Hello, world!";
        File testVolumePath = container.getTestVolumePath();
        if (testVolumePath != null) {
            String volumePath = testVolumePath.toString();
            try {
                ExecResult exec = container.execInContainer("bash", "-c", "echo -n \"" + template + "\" > /app/test.txt");
                assertEquals(0, exec.getExitCode());
                Path testPath = Paths.get(volumePath, "test.txt");
                assertTrue(testPath.toFile().exists());
                assertEquals("root", Files.getOwner(testPath).getName());
                List<String> allLines = Files.readAllLines(testPath);
                assertEquals(1, allLines.size());
                assertEquals(template, allLines.get(0));
            } finally {
                container.execInContainer("bash", "-c", "rm -f /app/test.txt");
            }
        }
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    void testUserPrivileges() throws IOException, InterruptedException {
        String template = "Hello, world!";
        File testVolumePath = container.getTestVolumePath();
        if (testVolumePath != null) {
            String volumePath = testVolumePath.toString();
            try {
                ExecResult exec = container.execInContainer("setuser", "system", "bash", "-c", "echo -n '" + template + "' > /app/test.txt");
                assertEquals(0, exec.getExitCode());
                Path testPath = Paths.get(volumePath, "test.txt");
                assertTrue(testPath.toFile().exists());
                assertEquals(System.getProperty("user.name"), Files.getOwner(testPath).getName());
                List<String> allLines = Files.readAllLines(testPath);
                assertEquals(1, allLines.size());
                assertEquals(template, allLines.get(0));
            } finally {
                container.execInContainer("bash", "-c", "rm -f /app/test.txt");
            }
        }
    }
}
