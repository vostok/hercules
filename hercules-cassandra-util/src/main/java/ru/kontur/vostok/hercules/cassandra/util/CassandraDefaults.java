package ru.kontur.vostok.hercules.cassandra.util;

/**
 * @author Gregory Koshelev
 */
public final class CassandraDefaults {
    // Default cassandra configuration

    public static final String DEFAULT_CASSANDRA_ADDRESS = "127.0.0.1";
    public static final int DEFAULT_CASSANDRA_PORT = 9042;
    public static final long DEFAULT_READ_TIMEOUT_MILLIS = 12_000L;
    public static final String DEFAULT_KEYSPACE = "hercules";
    public static final short DEFAULT_REPLICATION_FACTOR = 3;

    // Default pooling options:

    public static final int DEFAULT_CONNECTIONS_PER_HOST_LOCAL = 4;
    public static final int DEFAULT_CONNECTIONS_PER_HOST_REMOTE = 2;
    public static final int DEFAULT_MAX_REQUEST_PER_CONNECTION = 1024;

    private CassandraDefaults() {}
}
