package com.github.javister.docker.testing;

public class IllegalImageVariantException extends RuntimeException {
    public IllegalImageVariantException() {
    }

    public IllegalImageVariantException(String message) {
        super(message);
    }

    public IllegalImageVariantException(String message, Throwable cause) {
        super(message, cause);
    }

    public IllegalImageVariantException(Throwable cause) {
        super(cause);
    }
}
