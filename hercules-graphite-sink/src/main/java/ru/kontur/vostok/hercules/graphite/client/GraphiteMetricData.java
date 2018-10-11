package ru.kontur.vostok.hercules.graphite.client;

/**
 * Graphite metric data
 */
public class GraphiteMetricData {

    private String metricName;
    private long metricUnixTime;
    private double metricValue;

    public GraphiteMetricData(String metricName, long metricUnixTime, double metricValue) {
        this.metricName = metricName;
        this.metricUnixTime = metricUnixTime;
        this.metricValue = metricValue;
    }

    public String getMetricName() {
        return metricName;
    }

    public long getMetricUnixTime() {
        return metricUnixTime;
    }

    public double getMetricValue() {
        return metricValue;
    }
}
