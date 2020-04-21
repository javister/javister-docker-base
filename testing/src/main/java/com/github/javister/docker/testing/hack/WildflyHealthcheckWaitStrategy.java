package com.github.javister.docker.testing.hack;

import org.rnorth.ducttape.TimeoutException;
import org.rnorth.ducttape.unreliables.Unreliables;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.wait.strategy.AbstractWaitStrategy;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Wait strategy leveraging Docker's built-in healthcheck mechanism.
 *
 * @see <a href="https://docs.docker.com/engine/reference/builder/#healthcheck">https://docs.docker.com/engine/reference/builder/#healthcheck</a>
 */
public class WildflyHealthcheckWaitStrategy extends AbstractWaitStrategy {

    private final Container<?> container;

    public WildflyHealthcheckWaitStrategy(Container<?> container) {
        this.container = container;
    }

    @Override
    protected void waitUntilReady() {

        try {
            String host = DockerClientProviderStrategyHack.overrideHost.get();
            Unreliables.retryUntilSuccess((int) startupTimeout.getSeconds(), TimeUnit.SECONDS, () -> {
                Thread.sleep(10);
                if (host != null) {
                    return DockerClientProviderStrategyHack.onServerDo(host, waitStrategyTarget::isHealthy);
                }
                return waitStrategyTarget.isHealthy();
            });
        } catch (TimeoutException e) {
            try {
                container.execInContainer(
                        "setuser",
                        "system",
                        "/bin/sh",
                        "-c",
                        "jps | grep jboss-modules | awk '{ print $1 }' | xargs jstack -l > /config/wildfly.threaddump")
                        .getExitCode();
            } catch (IOException ex) {
                e.addSuppressed(ex);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
            throw new ContainerLaunchException("Timed out waiting for container to become healthy", e);
        }
    }
}
