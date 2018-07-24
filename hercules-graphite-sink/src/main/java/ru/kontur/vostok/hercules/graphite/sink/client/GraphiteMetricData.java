package ru.kontur.vostok.hercules.graphite.sink.client;

/**
 * Graphite metric data
 */
public class GraphiteMetricData {

    protected final long timestamp;
    protected final double value;

    public GraphiteMetricData(long timestamp, double value) {
        this.timestamp = timestamp;
        this.value = value;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public double getValue() {
        return value;
    }
}
