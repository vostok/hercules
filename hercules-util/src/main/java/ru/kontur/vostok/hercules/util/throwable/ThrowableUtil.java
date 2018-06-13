package ru.kontur.vostok.hercules.util.throwable;

import java.util.concurrent.Callable;

public final class ThrowableUtil {

    public static <V> V wrapException(Callable<V> callable) {
        try {
            return callable.call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
