package ru.kontur.vostok.hercules.cassandra.util;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.DefaultConsistencyLevel;
import com.datastax.oss.driver.api.core.ProtocolVersion;
import com.datastax.oss.driver.api.core.config.DefaultDriverOption;
import com.datastax.oss.driver.api.core.config.DriverConfigLoader;
import com.datastax.oss.driver.api.core.cql.BatchStatement;
import com.datastax.oss.driver.api.core.cql.BatchableStatement;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.DefaultBatchType;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import com.datastax.oss.driver.api.core.metadata.schema.KeyspaceMetadata;
import com.datastax.oss.driver.api.core.metadata.schema.TableMetadata;
import com.datastax.oss.driver.api.core.type.codec.registry.CodecRegistry;
import com.datastax.oss.driver.internal.core.metadata.DefaultEndPoint;
import com.datastax.oss.driver.internal.core.util.Sizes;
import com.datastax.oss.protocol.internal.PrimitiveSizes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.util.net.InetSocketAddressUtil;
import ru.kontur.vostok.hercules.util.properties.PropertyDescription;
import ru.kontur.vostok.hercules.util.properties.PropertyDescriptions;
import ru.kontur.vostok.hercules.util.validation.IntegerValidators;

import java.nio.ByteBuffer;
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

    private final int batchSizeBytesLimit;

    private volatile CqlSession session;
    private volatile int batchSizeBytesMinimum;

    public CassandraConnector(Properties properties) {
        this.dataCenter = Props.DATA_CENTER.extract(properties);
        this.nodes = Props.NODES.extract(properties);
        this.keyspace = Props.KEYSPACE.extract(properties);

        this.requestTimeoutMs = Props.REQUEST_TIMEOUT_MS.extract(properties);

        this.connectionsPerHostLocal = Props.CONNECTIONS_PER_HOST_LOCAL.extract(properties);
        this.connectionsPerHostRemote = Props.CONNECTIONS_PER_HOST_REMOTE.extract(properties);
        this.maxRequestsPerConnection = Props.MAX_REQUEST_PER_CONNECTION.extract(properties);

        this.consistencyLevel = Props.CONSISTENCY_LEVEL.extract(properties);

        this.batchSizeBytesLimit = Props.BATCH_SIZE_BYTES_LIMIT.extract(properties);
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

        BatchStatement batchStatement = BatchStatement.builder(DefaultBatchType.UNLOGGED).build();
        batchSizeBytesMinimum = batchStatement.computeSizeInBytes(session.getContext());
    }

    public CqlSession session() {
        return session;
    }

    public Optional<TableMetadata> metadata(final String table) {
        Optional<KeyspaceMetadata> keyspaceMetadata = session.getMetadata().getKeyspace(this.keyspace);
        return keyspaceMetadata.flatMap(x -> x.getTable(table));
    }

    /**
     * Return minimum {@link BatchStatement} size in bytes.
     *
     * @return minimum {@link BatchStatement} size in bytes
     */
    public int batchSizeBytesMinimum() {
        return batchSizeBytesMinimum;
    }

    /**
     * Return maximum {@link BatchStatement} size in bytes.
     *
     * @return maximum {@link BatchStatement} size in bytes
     */
    public int batchSizeBytesLimit() {
        return batchSizeBytesLimit;
    }

    /**
     * Compute an inner batch {@code statement} size in bytes.
     *
     * @param statement the inner statement of the batch
     * @return the inner statement size in bytes
     */
    public int computeInnerBatchStatementSizeBytes(BatchableStatement statement) {
        /*
        // Should be as simple as possible
        return Sizes.sizeOfInnerBatchStatementInBytes(statement, session.getContext().getProtocolVersion(), session.getContext().getCodecRegistry());
        // But Cassandra's driver has a bug https://datastax-oss.atlassian.net/browse/JAVA-2304
        // FIXME: replace with simple variant after releasing new version
         */
        ProtocolVersion protocolVersion = session.getContext().getProtocolVersion();
        CodecRegistry codecRegistry = session.getContext().getCodecRegistry();

        int size = 0;

        size += PrimitiveSizes.BYTE;

        if (statement instanceof SimpleStatement) {
            size += PrimitiveSizes.sizeOfLongString(((SimpleStatement) statement).getQuery());
            size +=
                    Sizes.sizeOfSimpleStatementValues(
                            ((SimpleStatement) statement), protocolVersion, codecRegistry);
        } else if (statement instanceof BoundStatement) {
            size +=
                    sizeOfShortBytes(
                            ((BoundStatement) statement).getPreparedStatement().getId());
            size += Sizes.sizeOfBoundStatementValues(((BoundStatement) statement));
        }
        return size;
    }

    //FIXME: Remove after releasing new version with https://github.com/datastax/native-protocol/pull/27
    private static int sizeOfShortBytes(ByteBuffer bytes) {
        return PrimitiveSizes.SHORT + (bytes == null ? 0 : bytes.remaining());
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

        static final PropertyDescription<Integer> BATCH_SIZE_BYTES_LIMIT =
                PropertyDescriptions.integerProperty("batchSizeBytesLimit").
                        withDefaultValue(CassandraDefaults.DEFAULT_BATCH_SIZE_BYTES_LIMIT).
                        withValidator(IntegerValidators.positive()).
                        build();
    }
}
