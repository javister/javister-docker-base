package com.github.javister.docker.testing.hack;

import com.github.dockerjava.api.DockerClient;
import org.testcontainers.containers.startupcheck.IsRunningStartupCheckStrategy;

public class IsRunningStartupCheckStrategyHack extends IsRunningStartupCheckStrategy {
    private String host;

    public IsRunningStartupCheckStrategyHack() {
        host = DockerClientProviderStrategyHack.overrideHost.get();
    }

    @Override
    public StartupStatus checkStartupState(DockerClient dockerClient, String containerId) {
        if (host == null) {
            return super.checkStartupState(dockerClient, containerId);
        }
        try {
            return DockerClientProviderStrategyHack.onServerDo(host, () -> super.checkStartupState(dockerClient, containerId));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
