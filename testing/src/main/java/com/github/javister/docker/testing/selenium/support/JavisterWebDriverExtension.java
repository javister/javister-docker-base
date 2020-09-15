package com.github.javister.docker.testing.selenium.support;

import com.github.javister.docker.testing.selenium.JavisterWebDriverContainer;
import com.github.javister.docker.testing.selenium.JavisterWebDriverProvider;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Stream;

public class JavisterWebDriverExtension implements
        AfterEachCallback,
        TestTemplateInvocationContextProvider {
    private final ConcurrentLinkedQueue<WebDriverParamResolver> browsers = new ConcurrentLinkedQueue<>();

    @Override
    public void afterEach(ExtensionContext context) {
        WebDriverParamResolver webDriverParamResolver = browsers.poll();
        while (webDriverParamResolver != null) {
            webDriverParamResolver.close();
            webDriverParamResolver = browsers.poll();
        }
    }

    @Override
    public boolean supportsTestTemplate(ExtensionContext context) {
        Optional<Method> testMethod = context.getTestMethod();
        if (!testMethod.isPresent()) {
            return false;
        }
        Optional<Class<?>> paramDetected = Arrays
                .stream(testMethod.get().getParameterTypes())
                .filter(clazz -> clazz == JavisterWebDriverContainer.class || clazz.isAssignableFrom(RemoteWebDriver.class))
                .findFirst();
        return paramDetected.isPresent();
    }

    @Override
    public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(ExtensionContext context) {
        Optional<Method> testMethod = context.getTestMethod();
        if (!testMethod.isPresent()) {
            return Stream.empty();
        }
        if (testMethod.get().isAnnotationPresent(JavisterWebDriverProvider.class)) {
            JavisterWebDriverProvider annotation = testMethod.get().getAnnotation(JavisterWebDriverProvider.class);
            if (annotation.value().length > 0) {
                return Stream.of(annotation.value())
                        .map(TemplateInvocationContext::new);
            } else {
                return Stream.of(JavisterWebDriverContainer.Browser.values())
                        .map(TemplateInvocationContext::new);
            }
        }
        return Stream.empty();
    }

    private class TemplateInvocationContext implements TestTemplateInvocationContext {
        private final JavisterWebDriverContainer.Browser browserType;

        public TemplateInvocationContext(JavisterWebDriverContainer.Browser browserType) {
            this.browserType = browserType;
        }

        @Override
        public String getDisplayName(int invocationIndex) {
            return String.format("[%d] %s", invocationIndex, browserType.name());
        }

        @Override
        public List<Extension> getAdditionalExtensions() {
            WebDriverParamResolver driverParamResolver = new WebDriverParamResolver(browserType);
            browsers.add(driverParamResolver);
            return Collections.singletonList(driverParamResolver);
        }
    }
}
