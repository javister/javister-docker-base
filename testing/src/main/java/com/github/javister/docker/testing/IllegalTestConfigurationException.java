package com.github.javister.docker.testing;

/**
 * Исключение, посылаемой в случае некорректной конфигурации TestContainers инфраструктуры.
 */
public class IllegalTestConfigurationException extends RuntimeException {
    /**
     * Создаёт новый экземпляр исключения.
     *
     * @param message Сообщение об ошибке.
     */
    public IllegalTestConfigurationException(String message) {
        super(message);
    }

    /**
     * Создаёт новый экземпляр исключения.
     *
     * @param message Сообщение об ошибке.
     * @param cause   исходная причина исключения.
     */
    public IllegalTestConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
