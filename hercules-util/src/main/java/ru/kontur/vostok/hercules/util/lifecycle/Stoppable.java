package ru.kontur.vostok.hercules.util.lifecycle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Class which implements {@link Stoppable} holds resources
 * that should be properly released before application exit.
 * <p>
 * Classes should not implement both {@link Stoppable} and {@link Closeable}
 * to void ambiguity of an object lifecycle.
 * Use fabric method {@link #from(Closeable)} to wrap {@link Closeable}.
 *
 * @author Gregory Koshelev
 * @see Lifecycle
 */
public interface Stoppable {
    Logger LOGGER = LoggerFactory.getLogger(Stoppable.class);

    boolean stop(long timeout, TimeUnit timeUnit);

    static Stoppable from(Closeable closeable) {
        return (timeout, unit) -> {
            try {
                closeable.close();
            } catch (IOException ex) {
                LOGGER.error("Closed with exception");
                return false;
            }
            return true;//FIXME: Replace with CompletableFuture + orTimeout when migrate to Java 11.
        };
    }
}
