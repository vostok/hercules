package ru.kontur.vostok.hercules.graphite.adapter;

/**
 * @author Gregory Koshelev
 */
public final class GraphiteAdapterDefaults {
    public static final String DEFAULT_HOST = "0.0.0.0";
    public static final int DEFAULT_PORT = 2003;
    public static final int DEFAULT_READ_TIMEOUT_MS = 90_000;

    private GraphiteAdapterDefaults() {
        /* static class */
    }
}
