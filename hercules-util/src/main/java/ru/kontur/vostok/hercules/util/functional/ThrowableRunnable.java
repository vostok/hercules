package ru.kontur.vostok.hercules.util.functional;

/**
 * {@link Runnable} analog with {@code throws} statement.
 *
 * @author Aleksandr Yuferov
 * @param <T> Exception type that can be thrown by {@link #run()} method.
 */
public interface ThrowableRunnable<T extends Throwable> {

    /**
     * Run.
     *
     * @throws T Method can throw exceptions on error.
     */
    void run() throws T;
}
