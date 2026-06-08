package com.personalblog.ragbackend.framework.exception;

/**
 * 客户端异常
 */
public class ClientException extends RuntimeException {
    public ClientException(String message) {
        super(message);
    }

    public ClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
