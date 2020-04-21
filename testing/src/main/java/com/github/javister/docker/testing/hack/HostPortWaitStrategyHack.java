package com.github.javister.docker.testing.hack;

import org.rnorth.ducttape.TimeoutException;
import org.rnorth.ducttape.unreliables.Unreliables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.wait.internal.ExternalPortListeningCheck;
import org.testcontainers.containers.wait.internal.InternalCommandPortListeningCheck;
import org.testcontainers.containers.wait.strategy.AbstractWaitStrategy;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Waits until a socket connection can be established on a port exposed or mapped by the container.
 *
 * @author richardnorth
 */
public class HostPortWaitStrategyHack extends AbstractWaitStrategy {
    private static final Logger log = LoggerFactory.getLogger(HostPortWaitStrategyHack.class);

    @Override
    protected void waitUntilReady() {
        final Set<Integer> externalLivenessCheckPorts = getLivenessCheckPorts();
        if (externalLivenessCheckPorts.isEmpty()) {
            log.debug("Liveness check ports of {} is empty. Not waiting.", waitStrategyTarget.getContainerInfo().getName());
            return;
        }

        @SuppressWarnings("unchecked")
        List<Integer> exposedPorts = waitStrategyTarget.getExposedPorts();

        final Set<Integer> internalPorts = getInternalPorts(externalLivenessCheckPorts, exposedPorts);

        Callable<Boolean> internalCheck = new InternalCommandPortListeningCheck(waitStrategyTarget, internalPorts);

        Callable<Boolean> externalCheck = new ExternalPortListeningCheck(waitStrategyTarget, externalLivenessCheckPorts);

        try {
            String host = DockerClientProviderStrategyHack.overrideHost.get();
            Unreliables.retryUntilSuccess((int) startupTimeout.getSeconds(), TimeUnit.SECONDS, () -> getRateLimiter().getWhenReady(() -> {
                    Thread.sleep(1);
                    if (host != null) {
                        return DockerClientProviderStrategyHack.onServerDo(host, () -> internalCheck.call() && externalCheck.call());
                    }
                    return internalCheck.call() && externalCheck.call();
                }));

        } catch (TimeoutException e) {
            throw new ContainerLaunchException("Timed out waiting for container port to open (" +
                    waitStrategyTarget.getContainerIpAddress() +
                    " ports: " +
                    externalLivenessCheckPorts +
                    " should be listening)", e);
        }
    }

    private Set<Integer> getInternalPorts(Set<Integer> externalLivenessCheckPorts, List<Integer> exposedPorts) {
        return exposedPorts.stream()
                .filter(it -> externalLivenessCheckPorts.contains(waitStrategyTarget.getMappedPort(it)))
                .collect(Collectors.toSet());
    }
}
