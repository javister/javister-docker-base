package com.github.javister.docker.testing.selenium.support;

import com.github.javister.docker.testing.TestRunException;
import com.github.javister.docker.testing.selenium.JavisterWebDriverConfigurator;
import com.github.javister.docker.testing.selenium.JavisterWebDriverContainer;
import com.github.javister.docker.testing.selenium.JavisterWebDriverContainer.Browser;
import com.github.javister.docker.testing.selenium.JavisterWebDriverProvider;
import io.qameta.allure.Allure;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;
import org.junit.platform.commons.support.AnnotationSupport;
import org.junit.platform.commons.support.HierarchyTraversalMode;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.testcontainers.lifecycle.TestDescription;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public class WebDriverParamResolver implements
        ParameterResolver,
        InvocationInterceptor,
        Closeable {
    private static final AtomicInteger counter = new AtomicInteger(1);
    private ExtensionContext context;
    private final Browser browserType;
    private JavisterWebDriverContainer container;
    private JavisterWebDriverProvider provider;

    public WebDriverParamResolver(Browser browserType) {
        this.browserType = browserType;
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        Class<?> clazz = parameterContext.getParameter().getType();
        return clazz == JavisterWebDriverContainer.class || clazz.isAssignableFrom(RemoteWebDriver.class);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        container = new JavisterWebDriverContainer().withDesiredCapabilities(browserType.getCapabilities());
        Optional<Method> testMethod = extensionContext.getTestMethod();
        provider = testMethod.map(method -> method.getAnnotation(JavisterWebDriverProvider.class)).orElse(null);
        configure(parameterContext, extensionContext, container);

        if (provider != null && provider.autostart()) {
            container.start();
        }
        if (parameterContext.getParameter().getType() == JavisterWebDriverContainer.class) {
            return container;
        }
        if (!container.isRunning()) {
            container.start();
        }
        return container.getWebDriver();
    }

    @Override
    public void interceptTestTemplateMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) throws Throwable {
        context = extensionContext;
        try {
            invocation.proceed();
        } finally {
            close();
        }
    }

    @Override
    public void close() {
        if (container != null && container.isRunning()) {
            try {
                if (context != null) {
                    TestDescription description = getDescription();
                    container.afterTest(description, context.getExecutionException());

                    attachVideo(description);
                }
            } finally {
                container.close();
                container = null;
            }
        }
    }

    public void attachVideo(TestDescription description) {
        URL workDir = context.getTestClass()
                .map(Class::getProtectionDomain)
                .map(domain -> domain.getCodeSource().getLocation())
                .orElse(null);

        if (detectAllure() && workDir != null && provider != null && provider.attachVideo()) {
            try {
                Files.list(Paths.get(workDir.toURI()).resolve(".."))
                        .filter(file -> file.toString().contains(description.getFilesystemFriendlyName()))
                        .forEach(file -> {
                            try (InputStream content = Files.newInputStream(file)) {
                                Allure.addAttachment(file.getFileName().toString(), content);
                            } catch (IOException e) {
                                throw new TestRunException("Can't attach the video files to Allure report", e);
                            }
                        });
            } catch (IOException | URISyntaxException e) {
                throw new TestRunException("Can't list test the work directory for video files", e);
            }
        }
    }

    private boolean detectAllure() {
        try {
            Class<?> allureClass = this.getClass().getClassLoader().loadClass("io.qameta.allure.Allure");
            return allureClass != null;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    @NotNull
    private TestDescription getDescription() {
        final Method testMethod = context.getTestMethod().orElse(null);
        final Class<?> clazz = context.getTestClass().orElse(null);
        if (testMethod != null && clazz != null) {
            return new TestDescription() {
                @Override
                public String getTestId() {
                    return context.getDisplayName();
                }

                @Override
                public String getFilesystemFriendlyName() {
                    return clazz.getName()
                            + context.getTestMethod().map(name -> "." + name.getName()).orElse("")
                            + "-" + browserType.name();
                }
            };
        }
        return new TestDescription() {
            @Override
            public String getTestId() {
                return context.getDisplayName();
            }

            @Override
            public String getFilesystemFriendlyName() {
                return Integer.toString(counter.get());
            }
        };
    }

    private void configure(ParameterContext parameterContext, ExtensionContext extensionContext, JavisterWebDriverContainer container) {
        annotatedConfigurator(extensionContext, parameterContext, container);
        classConfigurator(extensionContext, container);
    }

    private void classConfigurator(ExtensionContext extensionContext, JavisterWebDriverContainer container) {
        Optional<Method> testMethod = extensionContext.getTestMethod();
        if (!testMethod.isPresent()) {
            return;
        }
        if (provider != null) {
            Class<? extends JavisterWebDriverProvider.Configurator> configuratorClass = provider.configuratorClass();
            if (configuratorClass != JavisterWebDriverProvider.EmptyConfigurator.class) {
                try {
                    configuratorClass.newInstance().configure(container, browserType);
                } catch (InstantiationException | IllegalAccessException e) {
                    throw new TestRunException("Error during configurator method invocation", e);
                }
            }
        }
    }

    private void annotatedConfigurator(ExtensionContext extensionContext, ParameterContext parameterContext, JavisterWebDriverContainer container) {
        Optional<Class<?>> testClass = extensionContext.getTestClass();
        if (!testClass.isPresent()) {
            return;
        }
        List<Method> annotatedMethods = AnnotationSupport.findAnnotatedMethods(testClass.get(), JavisterWebDriverConfigurator.class, HierarchyTraversalMode.TOP_DOWN);
        for (Method annotatedMethod : annotatedMethods) {
            boolean containerPresent = false;
            Class<?>[] parameterTypes = annotatedMethod.getParameterTypes();
            Object[] params = new Object[parameterTypes.length];
            for (int i = 0; i < parameterTypes.length; i++) {
                if (parameterTypes[i] == JavisterWebDriverContainer.class) {
                    containerPresent = true;
                    params[i] = container;
                } else if (parameterTypes[i] == Browser.class) {
                    params[i] = browserType;
                } else if (parameterTypes[i] == ExtensionContext.class) {
                    params[i] = extensionContext;
                } else if (parameterTypes[i] == ParameterContext.class) {
                    params[i] = parameterContext;
                } else {
                    throw new IllegalArgumentException(
                            "Configurator method "
                                    + annotatedMethod.getDeclaringClass().getName()
                                    + "."
                                    + annotatedMethod.getName()
                                    + " have parameter of the unknown type "
                                    + parameterTypes[i].getName()
                    );
                }
            }
            if (!containerPresent) {
                throw new IllegalArgumentException(
                        "Configurator method "
                                + annotatedMethod.getDeclaringClass().getName()
                                + "."
                                + annotatedMethod.getName()
                                + " don't have parameter of type com.github.javister.docker.testing.selenium.JavisterWebDriverContainer"
                );
            }
            try {
                annotatedMethod.setAccessible(true);
                if (Modifier.isStatic(annotatedMethod.getModifiers())) {
                    annotatedMethod.invoke(null, params);
                } else {
                    Optional<Object> testInstance = extensionContext.getTestInstance();
                    if (!testInstance.isPresent()) {
                        throw new IllegalArgumentException(
                                "Can't invoke configurator method for unknown method instance of the test class: "
                                        + testClass.get().getName()
                        );
                    }
                    annotatedMethod.invoke(testInstance.get(), params);
                }
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new TestRunException("Error during configurator method invocation", e);
            }
        }
    }
}
