package com.github.javister.docker.testing;

/**
 * Ошибка работы инфраструктуры тестов на TestContainers.
 */
public class TestRunException extends RuntimeException {
    /**
     * Создаёт исключение с заданным описанием.
     *
     * @param message описание исключения.
     */
    public TestRunException(String message) {
        super(message);
    }

    /**
     * Создаёт исключение с заданными описанием и сходным исключением.
     *
     * @param message описание исключения.
     * @param cause   исходное исключение.
     */
    public TestRunException(String message, Throwable cause) {
        super(message, cause);
    }
}
