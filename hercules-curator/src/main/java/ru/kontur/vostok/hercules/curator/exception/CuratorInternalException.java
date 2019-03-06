package ru.kontur.vostok.hercules.curator.exception;

/**
 * @author Gregory Koshelev
 */
public class CuratorInternalException extends CuratorException {
    public CuratorInternalException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
