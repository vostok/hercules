package ru.kontur.vostok.hercules.health;

/**
 * Base interface for all Hercules metrics.
 *
 * @author Gregory Koshelev
 */
public interface Metric {
    /**
     * Represents metric name.
     *
     * @return  metric name
     */
    String name();
}
