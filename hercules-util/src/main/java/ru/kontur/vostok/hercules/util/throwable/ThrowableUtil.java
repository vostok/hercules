package ru.kontur.vostok.hercules.util.throwable;

import java.util.concurrent.Callable;

public final class ThrowableUtil {

    @FunctionalInterface
    public interface RunnableWithException {
        void run() throws Exception;
    }

    public static <V> V toUnchecked(Callable<V> callable) {
        try {
            return callable.call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void toUnchecked(RunnableWithException runnable) {
        try {
            runnable.run();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
