package ru.kontur.vostok.hercules.meta.curator.exception;

/**
 * @author Gregory Koshelev
 */
public class CuratorUnknownException extends CuratorException {
    public CuratorUnknownException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
