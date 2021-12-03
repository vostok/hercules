package ru.kontur.vostok.hercules.graphite.adapter.metric;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The metric is an event which has name, value and timestamp.
 * Optionally, it has tags — a set of key-value pairs.
 *
 * @author Gregory Koshelev
 */
public class Metric {
    private final byte[] name;
    private final MetricTag[] tags;
    private final double value;
    private final long timestamp;

    public Metric(@NotNull byte[] name, @Nullable MetricTag[] tags, double value, long timestamp) {
        this.name = name;
        this.tags = tags;
        this.value = value;
        this.timestamp = timestamp;
    }

    /**
     * The metric name.
     * <p>
     * Returns ASCII-encoded bytes.
     *
     * @return the metric name
     */
    @NotNull
    public byte[] name() {
        return name;
    }

    /**
     * Tags.
     * <p>
     * Returns {@code null} if metric doesn't have tags.
     *
     * @return tags
     */
    @Nullable
    public MetricTag[] tags() {
        return tags;
    }

    /**
     * The metric value.
     *
     * @return the metric value
     */
    public double value() {
        return value;
    }

    /**
     * The metric timestamp.
     * <p>
     * Returns metric timestamp in Unix Time (seconds have elapsed since 1970-01-01T00:00:00Z).
     *
     * @return the metric timestamp
     */
    public long timestamp() {
        return timestamp;
    }

    /**
     * Returns {@code true} if the metric has tags.
     *
     * @return {@code true} if the metric has tags
     */
    public boolean hasTags() {
        return tags != null;
    }
}
