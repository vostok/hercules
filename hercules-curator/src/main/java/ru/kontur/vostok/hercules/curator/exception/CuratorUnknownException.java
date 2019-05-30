package ru.kontur.vostok.hercules.curator.exception;

/**
 * @author Gregory Koshelev
 */
public class CuratorUnknownException extends CuratorException {
    public CuratorUnknownException(Throwable throwable) {
        super(throwable.getMessage(), throwable);
    }

    public CuratorUnknownException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
