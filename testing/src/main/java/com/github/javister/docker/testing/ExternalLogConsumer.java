package com.github.javister.docker.testing;

import org.testcontainers.containers.output.BaseConsumer;
import org.testcontainers.containers.output.OutputFrame;

import java.util.function.Consumer;

public class ExternalLogConsumer extends BaseConsumer<ExternalLogConsumer> {
    private Consumer<String> consumer;
    public ExternalLogConsumer(Consumer<String> consumer) {
        this.consumer = consumer;
    }

    @Override
    public void accept(OutputFrame outputFrame) {
        // TODO: Реализовать PR в основной проект Testcontainers
        consumer.accept(outputFrame.getUtf8String().replace("\u001B[m", ""));
    }
}
