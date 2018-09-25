package ru.kontur.vostok.hercules.graphite.client;

import java.util.Collection;
import java.util.Set;

/**
 * SimpleGraphiteClientRetryStrategy
 *
 * @author Kirill Sulim
 */
public class SimpleGraphiteClientRetryStrategy extends GraphiteClientRetryStrategy {

    private final int retryCount;
    private final Set<Class<? extends Exception>> suppressedExceptions;

    public SimpleGraphiteClientRetryStrategy(GraphiteMetricDataSender sender, int retryCount, Set<Class<? extends Exception>> suppressedExceptions) {
        super(sender);
        this.retryCount = retryCount;
        this.suppressedExceptions = suppressedExceptions;
    }

    @Override
    public void send(Collection<GraphiteMetricData> data) {
        int currentRetryCount = 0;
        while (currentRetryCount < retryCount) {
            try {
                sender.send(data);
                return;
            }
            catch (Exception e) {
                if (suppressedExceptions.contains(e.getClass())) {
                    currentRetryCount++;
                }
                else {
                    throw e;
                }
            }
        }
    }
}
