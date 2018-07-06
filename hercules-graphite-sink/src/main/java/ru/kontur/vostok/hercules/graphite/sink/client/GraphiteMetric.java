package ru.kontur.vostok.hercules.graphite.sink.client;

public class GraphiteMetric {

    protected final long timestamp;
    protected final double value;

    public GraphiteMetric(long timestamp, double value) {
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
