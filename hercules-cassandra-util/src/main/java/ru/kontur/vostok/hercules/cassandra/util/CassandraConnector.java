package ru.kontur.vostok.hercules.cassandra.util;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.HostDistance;
import com.datastax.driver.core.PoolingOptions;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.SocketOptions;
import com.datastax.driver.core.TableMetadata;
import ru.kontur.vostok.hercules.util.properties.PropertiesExtractor;

import java.util.Properties;

/**
 * @author Gregory Koshelev
 */
public class CassandraConnector {
    private final Properties properties;
    private final String keyspace;
    private volatile Cluster cluster;
    private volatile Session session;

    public CassandraConnector(Properties properties) {
        this.properties = properties;
        this.keyspace = properties.getProperty("keyspace", CassandraDefaults.DEFAULT_KEYSPACE);
    }

    public void connect() {
        final int connectionsPerHostLocal = PropertiesExtractor.get(properties, "connectionsPerHostLocal", CassandraDefaults.DEFAULT_CONNECTIONS_PER_HOST_LOCAL);
        final int connectionsPerHostRemote = PropertiesExtractor.get(properties, "connectionsPerHostRemote", CassandraDefaults.DEFAULT_CONNECTIONS_PER_HOST_REMOTE);
        final int maxRequestsPerConnectionLocal = PropertiesExtractor.get(properties, "maxRequestsPerConnectionLocal", CassandraDefaults.DEFAULT_MAX_REQUEST_PER_CONNECTION_LOCAL);
        final int maxRequestsPerConnectionRemote = PropertiesExtractor.get(properties, "maxRequestsPerConnectionRemote", CassandraDefaults.DEFAULT_MAX_REQUEST_PER_CONNECTION_REMOTE);

        final String node = properties.getProperty("node", CassandraDefaults.DEFAULT_CASSANDRA_ADDRESS);
        final int port = PropertiesExtractor.get(properties, "port", CassandraDefaults.DEFAULT_CASSANDRA_PORT);
        final int readTimeout = PropertiesExtractor.get(properties, "readTimeout", CassandraDefaults.DEFAULT_READ_TIMEOUT_MILLIS);

        Cluster.Builder builder = Cluster.builder().addContactPoint(node).withPort(port);

        PoolingOptions poolingOptions =
                new PoolingOptions()
                        .setConnectionsPerHost(HostDistance.LOCAL, connectionsPerHostLocal, connectionsPerHostLocal)
                        .setConnectionsPerHost(HostDistance.REMOTE, connectionsPerHostRemote, connectionsPerHostRemote)
                        .setMaxRequestsPerConnection(HostDistance.LOCAL, maxRequestsPerConnectionLocal)
                        .setMaxRequestsPerConnection(HostDistance.REMOTE, maxRequestsPerConnectionRemote);
        builder.withPoolingOptions(poolingOptions);

        SocketOptions socketOptions =
                new SocketOptions()
                        .setReadTimeoutMillis(readTimeout);
        builder.withSocketOptions(socketOptions);

        builder.withoutJMXReporting();

        cluster = builder.build();

        session = cluster.connect(keyspace);
    }

    public Session session() {
        return session;
    }

    public TableMetadata metadata(String table) {
        return cluster.getMetadata().getKeyspace(keyspace).getTable(table);
    }

    public void close() {
        try {
            if (session != null) {
                session.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            if (cluster != null) {
                cluster.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
