package ru.kontur.vostok.hercules.graphite.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
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
    public void send(Collection<GraphiteMetricData> data) throws IOException {
        int currentRetryCount = 0;
        IOException lastException;
        do {
            try {
                sender.send(data);
                return;
            } catch (IOException e) {
                lastException = e;
                LOGGER.error("Error on sending data to graphite", e);
                if (suppressedExceptions.contains(e.getClass())) {
                    currentRetryCount++;
                } else {
                    throw e;
                }
            }
        } while (currentRetryCount < retryCount);
        throw lastException;
    }
}
