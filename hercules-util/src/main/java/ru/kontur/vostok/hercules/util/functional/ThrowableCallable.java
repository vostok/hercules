package ru.kontur.vostok.hercules.util.functional;

/**
 * {@link java.util.concurrent.Callable} analog with typed {@code throws} statement.
 *
 * @author Aleksandr Yuferov
 * @param <T> Exception type that can be thrown by {@link #call()} method.
 * @param <R> Return type of {@link #call()} method.
 */
public interface ThrowableCallable<T extends Throwable, R> {

    /**
     * Call.
     *
     * @return Some data.
     * @throws T Throws some error.
     */
    R call() throws T;
}
