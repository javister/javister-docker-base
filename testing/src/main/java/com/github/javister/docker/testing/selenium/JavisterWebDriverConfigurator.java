package com.github.javister.docker.testing.selenium;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Аннотация, обозначающая метод для конфигурирования контецнеров WebDriver перед запуском тестов.
 * <p>Используется совместно с аннотацией {@link JavisterWebDriverProvider}.
 * <p>Метод, отмеченный данной аннотацией должен иметь как минимум один параметр типа
 * {@link JavisterWebDriverContainer}. Так же допускаются параметры следующих типов:
 * <ul>
 *     <li>{@link JavisterWebDriverContainer.Browser}
 *     <li>{@link ExtensionContext}
 *     <li>{@link ParameterContext}
 * </ul>
 * <p>Аннотируемый метод может быть как статическим, так и простым.
 *
 * @see JavisterWebDriverProvider
 */
@Inherited
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface JavisterWebDriverConfigurator {
}
