package com.github.javister.docker.testing.hack;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.dockerclient.AuditLoggingDockerClient;
import org.testcontainers.dockerclient.DockerClientConfigUtils;
import org.testcontainers.dockerclient.DockerClientProviderStrategy;
import org.testcontainers.dockerclient.InvalidConfigurationException;
import org.testcontainers.dockerclient.auth.AuthDelegatingDockerClientConfig;
import org.testcontainers.dockerclient.transport.okhttp.OkHttpDockerCmdExecFactory;
import org.testcontainers.utility.TestcontainersConfiguration;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Стратегия для хакинга Testcontainers.
 * <p>Необходимо для того, чтобы для Testcontainers можно было динамически указать целевой хост с Docker.
 * TODO: это временное решение, за неимением лучших вариантов. Разработчики Testcontainers обещают выделить основные
 *  функции фреймворка в отдельное ядро, что должно позволить обходиться без данных хаков. Необходимо мониторить ситуацию
 *  и избавиться от данного кода, как только представится такая возможность.
 */
public class DockerClientProviderStrategyHack extends DockerClientProviderStrategy {
    static ThreadLocal<String> overrideHost = new ThreadLocal<>();
    private static ThreadLocal<DockerClient> overrideClient = new ThreadLocal<>();
    private static ThreadLocal<DockerClientConfig> overrideConfig = new ThreadLocal<>();
    private static AtomicReference<DockerClientProviderStrategy> defaultStrategy = new AtomicReference<>();
    private static AtomicInteger initRecursionCounter = new AtomicInteger(0);

    public DockerClientProviderStrategyHack() {
        TestcontainersConfiguration.getInstance().updateGlobalConfig("docker.client.strategy", "");
    }

    @Override
    public void test() throws InvalidConfigurationException {
        if (defaultStrategy.get() == null) {
            try {
                initRecursionCounter.incrementAndGet();
                List<DockerClientProviderStrategy> configurationStrategies = new ArrayList<>();
                ServiceLoader.load(DockerClientProviderStrategy.class).forEach(configurationStrategies::add);
                DockerClientProviderStrategy strategy = DockerClientProviderStrategy.getFirstValidStrategy(configurationStrategies);
                defaultStrategy.compareAndSet(null, strategy);
            } finally {
                initRecursionCounter.decrementAndGet();
            }
        }

    }

    @Override
    public String getDescription() {
        return "### This is a hack!!! ### Host redirection for: " + defaultStrategy.get().getDescription();
    }

    @Override
    protected int getPriority() {
        return 10000;
    }

    @Override
    protected boolean isApplicable() {
        return initRecursionCounter.get() == 0;
    }

    @Override
    protected boolean isPersistable() {
        return false;
    }

    @Override
    public DockerClient getClient() {
        if (defaultStrategy.get() == null) {
            test();
        }
        return overrideClient.get() != null ? new AuditLoggingDockerClient(overrideClient.get()) : defaultStrategy.get().getClient();
    }

    @Override
    public String getDockerHostIpAddress() {
        return overrideConfig.get() != null ?
                DockerClientConfigUtils.getDockerHostIpAddress(overrideConfig.get()) :
                defaultStrategy.get().getDockerHostIpAddress();
    }

    public static void setContext(String dockerHost) {
        checkContext();
        overrideHost.set(dockerHost);
        overrideConfig.set(DefaultDockerClientConfig.createDefaultConfigBuilder().withDockerHost(dockerHost).build());
        overrideClient.set(getClientForConfigInt(overrideConfig.get()));

        try {
            Field strategy = DockerClientFactory.class.getDeclaredField("strategy");
            strategy.setAccessible(true);
            strategy.set(DockerClientFactory.instance(), new DockerClientProviderStrategyHack());
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static void resetContext() throws IOException {
        try {
            DockerClient client = overrideClient.get();
            if (client != null) {
                client.close();
            }
        } finally {
            overrideClient.set(null);
            overrideConfig.set(null);
            overrideHost.set(null);
        }
    }

    public static void onServerDo(String dockerHost, Runnable operation) throws IOException {
        try {
            setContext(dockerHost);
            operation.run();
        } finally {
            resetContext();
        }
    }

    public static <T> T onServerDo(String dockerHost, Callable<T> operation) throws Exception {
        checkContext();
        try {
            overrideHost.set(dockerHost);
            overrideConfig.set(DefaultDockerClientConfig.createDefaultConfigBuilder().withDockerHost(dockerHost).build());
            overrideClient.set(getClientForConfigInt(overrideConfig.get()));
            return operation.call();
        } finally {
            resetContext();
        }
    }

    private static DockerClient getClientForConfigInt(DockerClientConfig config) {
        DockerClientBuilder clientBuilder = DockerClientBuilder
                .getInstance(new AuthDelegatingDockerClientConfig(config));

        String transportType = TestcontainersConfiguration.getInstance().getTransportType();
        if ("okhttp".equals(transportType)) {
            clientBuilder
                    .withDockerCmdExecFactory(new OkHttpDockerCmdExecFactory());
        } else {
            throw new IllegalArgumentException("Unknown transport type: " + transportType);
        }

        LOGGER.trace("Will use '{}' transport", transportType);

        return clientBuilder.build();
    }

    private static void checkContext() {
        if (overrideClient.get() != null) {
            throw new IllegalStateException("Another command is being running in different context.");
        }
    }
}
