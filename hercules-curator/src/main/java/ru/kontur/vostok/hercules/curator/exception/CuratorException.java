package ru.kontur.vostok.hercules.curator.exception;

/**
 * @author Gregory Koshelev
 */
public abstract class CuratorException extends Exception {
    public CuratorException(String message) {
        super(message);
    }

    public CuratorException(Throwable cause) {
        super(cause);
    }

    public CuratorException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
