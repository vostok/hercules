package ru.kontur.vostok.hercules.cassandra.util;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.DefaultConsistencyLevel;
import com.datastax.oss.driver.api.core.ProtocolVersion;
import com.datastax.oss.driver.api.core.config.DefaultDriverOption;
import com.datastax.oss.driver.api.core.config.DriverConfigLoader;
import com.datastax.oss.driver.api.core.config.ProgrammaticDriverConfigLoaderBuilder;
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
import ru.kontur.vostok.hercules.util.parameter.Parameter;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;
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

    private final boolean authEnable;

    private CassandraAuthProvider cassandraAuthProvider;

    private volatile CqlSession session;
    private volatile int batchSizeBytesMinimum;

    public CassandraConnector(Properties properties) {
        this.dataCenter = PropertiesUtil.get(Props.DATA_CENTER, properties).get();
        this.nodes = PropertiesUtil.get(Props.NODES, properties).get();
        this.keyspace = PropertiesUtil.get(Props.KEYSPACE, properties).get();

        this.requestTimeoutMs = PropertiesUtil.get(Props.REQUEST_TIMEOUT_MS, properties).get();

        this.connectionsPerHostLocal = PropertiesUtil.get(Props.CONNECTIONS_PER_HOST_LOCAL, properties).get();
        this.connectionsPerHostRemote = PropertiesUtil.get(Props.CONNECTIONS_PER_HOST_REMOTE, properties).get();
        this.maxRequestsPerConnection = PropertiesUtil.get(Props.MAX_REQUEST_PER_CONNECTION, properties).get();

        this.consistencyLevel = PropertiesUtil.get(Props.CONSISTENCY_LEVEL, properties).get();

        this.batchSizeBytesLimit = PropertiesUtil.get(Props.BATCH_SIZE_BYTES_LIMIT, properties).get();

        this.authEnable = PropertiesUtil.get(Props.AUTH_ENABLE, properties).get();

        if (this.authEnable) {
            this.cassandraAuthProvider = new CassandraAuthProvider(PropertiesUtil.ofScope(properties, "auth.provider"));
        }
        init();
    }

    private void init() {
        ProgrammaticDriverConfigLoaderBuilder configLoaderBuilder = DriverConfigLoader.programmaticBuilder().
                withDuration(DefaultDriverOption.REQUEST_TIMEOUT, Duration.ofMillis(requestTimeoutMs)).
                withString(DefaultDriverOption.REQUEST_CONSISTENCY, consistencyLevel).
                withInt(DefaultDriverOption.CONNECTION_MAX_REQUESTS, maxRequestsPerConnection).
                withInt(DefaultDriverOption.CONNECTION_POOL_LOCAL_SIZE, connectionsPerHostLocal).
                withInt(DefaultDriverOption.CONNECTION_POOL_REMOTE_SIZE, connectionsPerHostRemote);

        if (authEnable) {
            configLoaderBuilder.
                    withString(DefaultDriverOption.AUTH_PROVIDER_CLASS, cassandraAuthProvider.getClassname()).
                    withString(DefaultDriverOption.AUTH_PROVIDER_USER_NAME, cassandraAuthProvider.getUsername()).
                    withString(DefaultDriverOption.AUTH_PROVIDER_PASSWORD, cassandraAuthProvider.getPassword());
        }

        DriverConfigLoader configLoader = configLoaderBuilder.build();

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
        static final Parameter<String> DATA_CENTER =
                Parameter.stringParameter("dataCenter").
                        withDefault(CassandraDefaults.DEFAULT_DATA_CENTER).
                        build();

        static final Parameter<String[]> NODES =
                Parameter.stringArrayParameter("nodes").
                        withDefault(new String[]{CassandraDefaults.DEFAULT_CASSANDRA_ADDRESS}).
                        build();

        static final Parameter<String> KEYSPACE =
                Parameter.stringParameter("keyspace").
                        withDefault(CassandraDefaults.DEFAULT_KEYSPACE).
                        build();

        static final Parameter<Long> REQUEST_TIMEOUT_MS =
                Parameter.longParameter("requestTimeoutMs").
                        withDefault(CassandraDefaults.DEFAULT_READ_TIMEOUT_MILLIS).
                        build();

        static final Parameter<Integer> CONNECTIONS_PER_HOST_LOCAL =
                Parameter.integerParameter("connectionsPerHostLocal").
                        withDefault(CassandraDefaults.DEFAULT_CONNECTIONS_PER_HOST_LOCAL).
                        build();

        static final Parameter<Integer> CONNECTIONS_PER_HOST_REMOTE =
                Parameter.integerParameter("connectionsPerHostRemote").
                        withDefault(CassandraDefaults.DEFAULT_CONNECTIONS_PER_HOST_REMOTE).
                        build();

        static final Parameter<Integer> MAX_REQUEST_PER_CONNECTION =
                Parameter.integerParameter("maxRequestsPerConnection").
                        withDefault(CassandraDefaults.DEFAULT_MAX_REQUEST_PER_CONNECTION).
                        build();

        static final Parameter<String> CONSISTENCY_LEVEL =
                Parameter.stringParameter("consistencyLevel").
                        withDefault(DefaultConsistencyLevel.QUORUM.name()).
                        build();

        static final Parameter<Integer> BATCH_SIZE_BYTES_LIMIT =
                Parameter.integerParameter("batchSizeBytesLimit").
                        withDefault(CassandraDefaults.DEFAULT_BATCH_SIZE_BYTES_LIMIT).
                        withValidator(IntegerValidators.positive()).
                        build();

        static final Parameter<Boolean> AUTH_ENABLE =
                Parameter.booleanParameter("auth.enable").
                        withDefault(CassandraDefaults.DEFAULT_AUTH_ENABLE).
                        build();
    }
}
