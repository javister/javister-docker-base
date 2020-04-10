package com.github.javister.docker.testing;

public class IllegalExecResultException extends RuntimeException {
    public IllegalExecResultException(String message) {
        super(message);
    }

    public IllegalExecResultException(String message, Throwable cause) {
        super(message, cause);
    }
}
