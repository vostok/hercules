package ru.kontur.vostok.hercules.curator.exception;

/**
 * @author Gregory Koshelev
 */
public class CuratorConnectionException extends CuratorException {
    public CuratorConnectionException(Throwable cause) {
        super(cause);
    }

    public CuratorConnectionException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
