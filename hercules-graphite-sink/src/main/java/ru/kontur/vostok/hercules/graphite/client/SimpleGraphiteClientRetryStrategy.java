package ru.kontur.vostok.hercules.graphite.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Set;

/**
 * SimpleGraphiteClientRetryStrategy
 *
 * @author Kirill Sulim
 */
public class SimpleGraphiteClientRetryStrategy extends GraphiteClientRetryStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleGraphiteClientRetryStrategy.class);

    /**
     * Maximum retry count
     */
    private final int retryCount;

    /**
     * Set of non critical exceptions which can be retried.
     */
    private final Set<Class<? extends Throwable>> suppressedExceptions;

    public SimpleGraphiteClientRetryStrategy(GraphiteMetricDataSender sender, int retryCount, Set<Class<? extends Throwable>> suppressedExceptions) {
        super(sender);
        this.retryCount = retryCount;
        this.suppressedExceptions = suppressedExceptions;
    }

    @Override
    public void send(Collection<GraphiteMetricData> data) {
        int currentRetryCount = 0;
        RuntimeException lastException;
        do {
            try {
                sender.send(data);
                return;
            }
            catch (RuntimeException e) {
                lastException = e;
                LOGGER.error("Error on sending data to graphite", e);
                if (suppressedExceptions.contains(unwrapRuntimeException(e).getClass())) {
                    currentRetryCount++;
                }
                else {
                    throw e;
                }
            }
        } while (currentRetryCount < retryCount);
        throw lastException;
    }

    /*
     * Unwraps runtime exception up to original runtime exception or first not RuntimeException object
     */
    private static Throwable unwrapRuntimeException(RuntimeException e) {
        Throwable t = e;
        while (t.getClass().equals(RuntimeException.class) && t.getCause() != t) {
            t = t.getCause();
        }
        return t;
    }
}
