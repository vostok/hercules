package ru.kontur.vostok.hercules.util.application.shutdown;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Deque;
import java.util.LinkedList;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * StackedStopper - stops all added stoppable instances in reverse order
 *
 * @author Kirill Sulim
 */
public class StackedStopper implements Stoppable {

    private static class StoppableWithErrorHandler {
        private final Stoppable stoppable;
        private final Consumer<Throwable> handler;

        public StoppableWithErrorHandler(Stoppable stoppable, Consumer<Throwable> handler) {
            this.stoppable = stoppable;
            this.handler = handler;
        }
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(StackedStopper.class);

    private final Deque<StoppableWithErrorHandler> stack = new LinkedList<>();
    private final Stoppable loggerStopper;

    /**
     * Logger must be stopped the last and need special handling so it should be passed in constructor
     *
     * @param loggerStopper logger stopper instance
     */
    public StackedStopper(Stoppable loggerStopper) {
        this.loggerStopper = loggerStopper;
    }

    /**
     * Add stoppable without throwable handler, so all throwable will be ignored
     *
     * @param stoppable stoppable instance
     */
    public void add(Stoppable stoppable) {
        add(stoppable, null);
    }

    /**
     * Add stoppable with throwable handler
     *
     * @param stoppable stoppable
     * @param handler throwable handler
     */
    public void add(Stoppable stoppable, Consumer<Throwable> handler) {
        stack.push(new StoppableWithErrorHandler(stoppable, handler));
    }

    @Override
    public void stop(long timeout, TimeUnit timeUnit) {
        long start = System.currentTimeMillis();
        LOGGER.info("Starting shutdown");
        while (!stack.isEmpty()) {
            StoppableWithErrorHandler stoppableWithErrorHandler = stack.pop();
            Stoppable stoppable = stoppableWithErrorHandler.stoppable;
            Consumer<Throwable> handler = stoppableWithErrorHandler.handler;
            String name = stoppable.getClass().toString();
            try {
                stoppable.stop(timeout, timeUnit);
                LOGGER.debug(String.format("Stopped '%s'", name));
            }
            catch (Throwable t) {
                LOGGER.error(String.format("Error on stopping '%s'", name), t);
                if (Objects.nonNull(handler)) {
                    handler.accept(t);
                }
                else {
                    /* Skip */
                }
            }
        }
        LOGGER.info("Shutdown terminated, elapsed time " + (System.currentTimeMillis() - start) + " millis");
        if (Objects.nonNull(loggerStopper)) {
            loggerStopper.stop(0, TimeUnit.MILLISECONDS);
        }
    }
}
