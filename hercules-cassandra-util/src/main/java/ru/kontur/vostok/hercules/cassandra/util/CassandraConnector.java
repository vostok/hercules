package ru.kontur.vostok.hercules.cassandra.util;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.HostDistance;
import com.datastax.driver.core.PoolingOptions;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.SocketOptions;
import com.datastax.driver.core.TableMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.util.properties.PropertyDescription;
import ru.kontur.vostok.hercules.util.properties.PropertyDescriptions;
import ru.kontur.vostok.hercules.util.validation.Validators;

import java.util.Properties;

/**
 * @author Gregory Koshelev
 */
public class CassandraConnector {
    private static final Logger LOGGER = LoggerFactory.getLogger(CassandraConnector.class);

    private final String keyspace;

    private final int connectionsPerHostLocal;
    private final int connectionsPerHostRemote;
    private final int maxRequestsPerConnectionLocal;
    private final int maxRequestsPerConnectionRemote;

    private final String[] nodes;
    private final int port;
    private final int readTimeoutMs;

    private volatile Cluster cluster;
    private volatile Session session;


    public CassandraConnector(Properties properties) {
        this.keyspace = Props.KEYSPACE.extract(properties);

        this.connectionsPerHostLocal = Props.CONNECTIONS_PER_HOST_LOCAL.extract(properties);
        this.connectionsPerHostRemote = Props.CONNECTIONS_PER_HOST_REMOTE.extract(properties);
        this.maxRequestsPerConnectionLocal = Props.MAX_REQUEST_PER_CONNECTION_LOCAL.extract(properties);
        this.maxRequestsPerConnectionRemote = Props.MAX_REQUEST_PER_CONNECTION_REMOTE.extract(properties);

        this.nodes = Props.NODES.extract(properties);
        this.port = Props.PORT.extract(properties);
        this.readTimeoutMs = Props.READ_TIMEOUT_MS.extract(properties);
    }

    public void connect() {
        Cluster.Builder builder = Cluster.builder().addContactPoints(nodes).withPort(port);

        PoolingOptions poolingOptions =
                new PoolingOptions()
                        .setConnectionsPerHost(HostDistance.LOCAL, connectionsPerHostLocal, connectionsPerHostLocal)
                        .setConnectionsPerHost(HostDistance.REMOTE, connectionsPerHostRemote, connectionsPerHostRemote)
                        .setMaxRequestsPerConnection(HostDistance.LOCAL, maxRequestsPerConnectionLocal)
                        .setMaxRequestsPerConnection(HostDistance.REMOTE, maxRequestsPerConnectionRemote);
        builder.withPoolingOptions(poolingOptions);

        SocketOptions socketOptions =
                new SocketOptions()
                        .setReadTimeoutMillis(readTimeoutMs);
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
            LOGGER.error("Error on closing session", e);
        }
        try {
            if (cluster != null) {
                cluster.close();
            }
        } catch (Exception e) {
            LOGGER.error("Error on closing cluster", e);
        }
    }

    private static class Props {
        static final PropertyDescription<String> KEYSPACE = PropertyDescriptions
                .stringProperty("keyspace")
                .withDefaultValue(CassandraDefaults.DEFAULT_KEYSPACE)
                .build();

        static final PropertyDescription<Integer> CONNECTIONS_PER_HOST_LOCAL = PropertyDescriptions
                .integerProperty("connectionsPerHostLocal")
                .withDefaultValue(CassandraDefaults.DEFAULT_CONNECTIONS_PER_HOST_LOCAL)
                .build();

        static final PropertyDescription<Integer> CONNECTIONS_PER_HOST_REMOTE = PropertyDescriptions
                .integerProperty("connectionsPerHostRemote")
                .withDefaultValue(CassandraDefaults.DEFAULT_CONNECTIONS_PER_HOST_REMOTE)
                .build();

        static final PropertyDescription<Integer> MAX_REQUEST_PER_CONNECTION_LOCAL = PropertyDescriptions
                .integerProperty("maxRequestsPerConnectionLocal")
                .withDefaultValue(CassandraDefaults.DEFAULT_MAX_REQUEST_PER_CONNECTION_LOCAL)
                .build();

        static final PropertyDescription<Integer> MAX_REQUEST_PER_CONNECTION_REMOTE = PropertyDescriptions
                .integerProperty("maxRequestsPerConnectionRemote")
                .withDefaultValue(CassandraDefaults.DEFAULT_MAX_REQUEST_PER_CONNECTION_REMOTE)
                .build();

        static final PropertyDescription<String[]> NODES = PropertyDescriptions
                .arrayOfStringsProperty("nodes")
                .withDefaultValue(new String[]{CassandraDefaults.DEFAULT_CASSANDRA_ADDRESS})
                .build();

        static final PropertyDescription<Integer> PORT = PropertyDescriptions
                .integerProperty("port")
                .withDefaultValue(CassandraDefaults.DEFAULT_CASSANDRA_PORT)
                .withValidator(Validators.portValidator())
                .build();

        static final PropertyDescription<Integer> READ_TIMEOUT_MS = PropertyDescriptions
                .integerProperty("readTimeoutMs")
                .withDefaultValue(CassandraDefaults.DEFAULT_READ_TIMEOUT_MILLIS)
                .build();
    }
}
