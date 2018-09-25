package ru.kontur.vostok.hercules.graphite.client;

import java.util.Collection;

/**
 * GraphiteClientRetryStrategy
 *
 * @author Kirill Sulim
 */
public abstract class GraphiteClientRetryStrategy implements GraphiteMetricDataSender {

    protected final GraphiteMetricDataSender sender;

    public GraphiteClientRetryStrategy(GraphiteMetricDataSender sender) {
        this.sender = sender;
    }

    @Override
    public abstract void send(Collection<GraphiteMetricData> data);
}
