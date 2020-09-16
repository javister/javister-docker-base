package com.github.javister.docker.testing.selenium;

import com.github.javister.docker.testing.selenium.JavisterWebDriverContainer.Browser;
import com.github.javister.docker.testing.selenium.support.JavisterWebDriverExtension;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.WebDriver;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Аннотация для инжектирования {@link JavisterWebDriverContainer} или {@link WebDriver} в метод теста.
 * <p>По умолчанию, проаннотированный метод будет вызван два раза: для браузеров Chrome и Firefox.
 * Если требуется вызвать тест только с одним из этих браузеров, то необходимо задать поле
 * {@link JavisterWebDriverProvider#value()} в соответствующее значение.
 *
 * <p>По умолчанию контейнер с WebDriver запускается перед вызовом метода теста. Это поведение можно изменить
 * путём установки свойства {@link JavisterWebDriverProvider#autostart()}.
 *
 * <p>Если контейнер с WebDriver необходимо донастроить перед запуском, то можно
 * воспользоваться двумя способами:
 * <ul>
 *     <li>Создать метод, принимающий параметром объект типа {@link JavisterWebDriverContainer}
 *         и проаннотировать его с помощью {@link JavisterWebDriverConfigurator}.
 *         Этот способ подходит, если все создаваемые контейнеры с WebDriver в данном классе теста
 *         должны иметь одинаковые настройки.
 *     <li>Создать класс, имплементирующий {@link JavisterWebDriverProvider.Configurator}
 *         и присвоить его в свойство {@link JavisterWebDriverProvider#configuratorClass()}.
 *         Данный метод хорош, если для какого-то тестового метода необходимы индивидуальные настройки.
 * </ul>
 * <p>Если в тестовом классе одновременно используются оба метода, то сначала буде вызван метод,
 * проаннотированный с помощью {@link JavisterWebDriverConfigurator}, а потом имплементация
 * {@link JavisterWebDriverProvider.Configurator}. И туда и туда будет передан один и тот же
 * объект контейнера для настройки.
 *
 * @see JavisterWebDriverConfigurator
 * @see JavisterWebDriverProvider.Configurator
 */
@Inherited
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@TestTemplate
@ExtendWith(JavisterWebDriverExtension.class)
public @interface JavisterWebDriverProvider {
    /**
     * Перечисление браузеров для тестирования.
     * <p>Если данное значение не установлено, то это эквивалентно перечислению
     * всех вариантов из {@link Browser}.
     * Проаннотированный данной аннотацией метод теста будет вызван последовательно
     * для каждого элемента из массива данного свойства.
     *
     * @return Перечисление браузеров для тестирования.
     */
    Browser[] value() default {};

    /**
     * Флаг, обозначающий необходимость запуска контейнера, перед вызовом проаннотированного метода.
     *
     * @return флаг, обозначающий необходимость запуска контейнера, перед вызовом проаннотированного метода.
     */
    boolean autostart() default true;

    /**
     * Класс конфигуратора контейнера с WebDriver, имплементирующий {@link JavisterWebDriverProvider.Configurator}.
     *
     * @return класс конфигуратора контейнера с WebDriver, имплементирующий {@link JavisterWebDriverProvider.Configurator}.
     */
    Class<? extends Configurator> configuratorClass() default EmptyConfigurator.class;

    /**
     * Флаг, обозначающий необходимость добавления файла с записью теста к отчёту Allure.
     *
     * @return флаг, обозначающий необходимость добавления файла с записью теста к отчёту Allure.
     */
    boolean attachVideo() default true;

    /**
     * Интерфейс конфигуратора для настройки контейнеров WebDriver.
     */
    interface Configurator {
        /**
         * Конфигурирование контейнера WebDriver.
         * <p>Имплементации данного метода используются для настройки контейнеров перед запуском тестов.
         *
         * @param webContainer контейнер, требующий настройки перед запуском.
         * @param type         тип браузера, для которого создаётся контейнер.
         */
        void configure(JavisterWebDriverContainer webContainer, Browser type);
    }

    class EmptyConfigurator implements Configurator {
        @Override
        public void configure(JavisterWebDriverContainer webContainer, Browser type) {
            // Do nothing
        }
    }
}
