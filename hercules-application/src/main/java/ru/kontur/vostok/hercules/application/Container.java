package ru.kontur.vostok.hercules.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.util.lifecycle.Lifecycle;
import ru.kontur.vostok.hercules.util.lifecycle.Stoppable;
import ru.kontur.vostok.hercules.util.time.TimeSource;
import ru.kontur.vostok.hercules.util.time.Timer;

import java.io.Closeable;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;

/**
 * Container registers components and provides graceful shutdown in a reversed order (LIFO fashion).
 *
 * @author Gregory Koshelev
 */
public class Container {

    private static final Logger LOGGER = LoggerFactory.getLogger(Container.class);

    private final ConcurrentLinkedDeque<Stoppable> components = new ConcurrentLinkedDeque<>();

    /**
     * Register and start the component.
     * <p>
     * Registered component will properly stopped on application shutdown in an appropriate moment. Components are stopped in LIFO fashion.
     *
     * @param component the component
     * @param <T>       implements {@link Lifecycle}
     * @return the component
     * @see #register(Stoppable)
     */
    public <T extends Lifecycle> T register(T component) {
        components.push(component);
        component.start();
        return component;
    }

    /**
     * Register the component.
     * <p>
     * Registered component will properly stopped on application shutdown in an appropriate moment. Components are stopped in LIFO fashion.
     *
     * @param component the component
     * @param <T>       implements {@link Stoppable}
     * @return the component
     * @see #register(Lifecycle)
     */
    public <T extends Stoppable> T register(T component) {
        components.push(component);
        return component;
    }

    /**
     * Register the component.
     * <p>
     * Registered component will properly closed on application shutdown in an appropriate moment. Components are stopped in LIFO fashion.
     *
     * @param component the component
     * @param <T>       implements Closeable
     * @return the component
     */
    public <T extends Closeable> T register(T component) {
        components.push(Stoppable.from(component));
        return component;
    }

    /**
     * Stop components one-by-one in LIFO fashion.
     * <p>
     * The method is package-private to avoid possible bugs when it is called externally.
     *
     * @param timeout the timeout
     * @param unit    the time unit
     * @return {@code true} if successfully stopped all components
     */
    boolean stop(long timeout, TimeUnit unit) {
        Timer timer = TimeSource.SYSTEM.timer(unit.toMillis(timeout));
        boolean stopped = true;
        for (Stoppable component : components) {
            try {
                boolean result = component.stop(timer.remainingTimeMs(), TimeUnit.MILLISECONDS);
                if (!result) {
                    LOGGER.warn("Component " + component.getClass() + " has not been stopped properly");
                }
                stopped = stopped && result;
            } catch (Throwable t) {
                LOGGER.error("Error on stopping " + component.getClass(), t);
                stopped = false;
            }
        }
        return stopped;
    }
}
