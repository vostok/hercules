package ru.kontur.vostok.hercules.clickhouse.util;

/**
 * ClickHouse connector defaults.
 *
 * @author Gregory Koshelev
 */
public final class ClickHouseDefaults {
    /**
     * Default ClickHouse node (host:port).
     */
    public static final String DEFAULT_NODE = "localhost:8123";
    /**
     * Default ClickHouse database name.
     */
    public static final String DEFAULT_DB = "default";
    /**
     * Default connection validation interval in millis.
     */
    public static final long DEFAULT_VALIDATION_INTERVAL_MS = 10_000;

    private ClickHouseDefaults() {
        /* static class */
    }
}
