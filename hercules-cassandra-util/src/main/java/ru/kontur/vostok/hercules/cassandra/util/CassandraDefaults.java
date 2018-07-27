package ru.kontur.vostok.hercules.cassandra.util;

import com.datastax.driver.core.ProtocolOptions;
import com.datastax.driver.core.SocketOptions;

/**
 * @author Gregory Koshelev
 */
public final class CassandraDefaults {
    // Default cassandra configuration

    public static final String DEFAULT_CASSANDRA_ADDRESS = "127.0.0.1";
    public static final int DEFAULT_CASSANDRA_PORT = ProtocolOptions.DEFAULT_PORT;
    public static final int DEFAULT_READ_TIMEOUT_MILLIS = SocketOptions.DEFAULT_READ_TIMEOUT_MILLIS;
    public static final String DEFAULT_KEYSPACE = "hercules";
    public static final short DEFAULT_REPLICATION_FACTOR = 3;

    // Default pooling options:

    public static final int DEFAULT_CONNECTIONS_PER_HOST_LOCAL = 4;
    public static final int DEFAULT_CONNECTIONS_PER_HOST_REMOTE = 2;
    public static final int DEFAULT_MAX_REQUEST_PER_CONNECTION_LOCAL = 1024;
    public static final int DEFAULT_MAX_REQUEST_PER_CONNECTION_REMOTE = 256;

    private CassandraDefaults() {}
}
