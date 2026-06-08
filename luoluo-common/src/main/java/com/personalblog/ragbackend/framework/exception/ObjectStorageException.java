package com.personalblog.ragbackend.framework.exception;

/**
 * Object存储异常
 */
public class ObjectStorageException extends RuntimeException {
    public ObjectStorageException(String message) {
        super(message);
    }

    public ObjectStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
