package ru.kontur.vostok.hercules.cassandra.util;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.DefaultConsistencyLevel;
import com.datastax.oss.driver.api.core.config.DefaultDriverOption;
import com.datastax.oss.driver.api.core.config.DriverConfigLoader;
import com.datastax.oss.driver.api.core.metadata.schema.KeyspaceMetadata;
import com.datastax.oss.driver.api.core.metadata.schema.TableMetadata;
import com.datastax.oss.driver.internal.core.metadata.DefaultEndPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.util.net.InetSocketAddressUtil;
import ru.kontur.vostok.hercules.util.properties.PropertyDescription;
import ru.kontur.vostok.hercules.util.properties.PropertyDescriptions;

import java.time.Duration;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Gregory Koshelev
 */
public class CassandraConnector {
    private static final Logger LOGGER = LoggerFactory.getLogger(CassandraConnector.class);

    private final String dataCenter;
    private final String[] nodes;
    private final String keyspace;

    private final long requestTimeoutMs;

    private final int connectionsPerHostLocal;
    private final int connectionsPerHostRemote;
    private final int maxRequestsPerConnection;

    private final String consistencyLevel;

    private volatile CqlSession session;


    public CassandraConnector(Properties properties) {
        this.dataCenter = Props.DATA_CENTER.extract(properties);
        this.nodes = Props.NODES.extract(properties);
        this.keyspace = Props.KEYSPACE.extract(properties);

        this.requestTimeoutMs = Props.REQUEST_TIMEOUT_MS.extract(properties);

        this.connectionsPerHostLocal = Props.CONNECTIONS_PER_HOST_LOCAL.extract(properties);
        this.connectionsPerHostRemote = Props.CONNECTIONS_PER_HOST_REMOTE.extract(properties);
        this.maxRequestsPerConnection = Props.MAX_REQUEST_PER_CONNECTION.extract(properties);

        this.consistencyLevel = Props.CONSISTENCY_LEVEL.extract(properties);
    }

    public void connect() {
        DriverConfigLoader configLoader = DriverConfigLoader.programmaticBuilder().
                withDuration(DefaultDriverOption.REQUEST_TIMEOUT, Duration.ofMillis(requestTimeoutMs)).
                withString(DefaultDriverOption.REQUEST_CONSISTENCY, consistencyLevel).
                withInt(DefaultDriverOption.CONNECTION_MAX_REQUESTS, maxRequestsPerConnection).
                withInt(DefaultDriverOption.CONNECTION_POOL_LOCAL_SIZE, connectionsPerHostLocal).
                withInt(DefaultDriverOption.CONNECTION_POOL_REMOTE_SIZE, connectionsPerHostRemote).
                build();

        session = CqlSession.builder().
                withLocalDatacenter(dataCenter).
                addContactEndPoints(
                        Stream.of(nodes).
                                map(x -> new DefaultEndPoint(InetSocketAddressUtil.fromString(x, CassandraDefaults.DEFAULT_CASSANDRA_PORT))).
                                collect(Collectors.toList())).
                withKeyspace(keyspace).
                withConfigLoader(configLoader).
                build();
    }

    public CqlSession session() {
        return session;
    }

    public Optional<TableMetadata> metadata(final String table) {
        Optional<KeyspaceMetadata> keyspaceMetadata = session.getMetadata().getKeyspace(this.keyspace);
        return keyspaceMetadata.flatMap(x -> x.getTable(table));
    }

    public void close() {
        try {
            if (session != null) {
                session.close();
            }
        } catch (Exception e) {
            LOGGER.error("Error on closing session", e);
        }
    }

    private static class Props {
        static final PropertyDescription<String> DATA_CENTER =
                PropertyDescriptions.stringProperty("dataCenter").
                        withDefaultValue(CassandraDefaults.DEFAULT_DATA_CENTER).
                        build();

        static final PropertyDescription<String[]> NODES =
                PropertyDescriptions.arrayOfStringsProperty("nodes").
                        withDefaultValue(new String[]{CassandraDefaults.DEFAULT_CASSANDRA_ADDRESS}).
                        build();

        static final PropertyDescription<String> KEYSPACE =
                PropertyDescriptions.stringProperty("keyspace").
                        withDefaultValue(CassandraDefaults.DEFAULT_KEYSPACE).
                        build();

        static final PropertyDescription<Long> REQUEST_TIMEOUT_MS =
                PropertyDescriptions.longProperty("requestTimeoutMs").
                        withDefaultValue(CassandraDefaults.DEFAULT_READ_TIMEOUT_MILLIS).
                        build();

        static final PropertyDescription<Integer> CONNECTIONS_PER_HOST_LOCAL =
                PropertyDescriptions.integerProperty("connectionsPerHostLocal").
                        withDefaultValue(CassandraDefaults.DEFAULT_CONNECTIONS_PER_HOST_LOCAL).
                        build();

        static final PropertyDescription<Integer> CONNECTIONS_PER_HOST_REMOTE =
                PropertyDescriptions.integerProperty("connectionsPerHostRemote").
                        withDefaultValue(CassandraDefaults.DEFAULT_CONNECTIONS_PER_HOST_REMOTE).
                        build();

        static final PropertyDescription<Integer> MAX_REQUEST_PER_CONNECTION =
                PropertyDescriptions.integerProperty("maxRequestsPerConnection").
                        withDefaultValue(CassandraDefaults.DEFAULT_MAX_REQUEST_PER_CONNECTION).
                        build();

        static final PropertyDescription<String> CONSISTENCY_LEVEL =
                PropertyDescriptions.stringProperty("consistencyLevel").
                        withDefaultValue(DefaultConsistencyLevel.QUORUM.name()).
                        build();
    }
}
