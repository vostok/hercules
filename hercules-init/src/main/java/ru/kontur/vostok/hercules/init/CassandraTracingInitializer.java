package ru.kontur.vostok.hercules.init;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.internal.core.metadata.DefaultEndPoint;
import ru.kontur.vostok.hercules.cassandra.util.CassandraDefaults;
import ru.kontur.vostok.hercules.util.net.InetSocketAddressUtil;
import ru.kontur.vostok.hercules.util.parameter.Parameter;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;
import ru.kontur.vostok.hercules.util.validation.IntegerValidators;

import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Gregory Koshelev
 */
public class CassandraTracingInitializer {
    private final String dataCenter;
    private final String[] nodes;
    private final String keyspace;
    private final String tableName;
    private final int replicationFactor;
    private final int ttl;

    public CassandraTracingInitializer(Properties properties) {
        this.dataCenter = PropertiesUtil.get(Props.DATA_CENTER, properties).get();
        this.nodes = PropertiesUtil.get(Props.NODES, properties).get();
        this.keyspace = PropertiesUtil.get(Props.KEYSPACE, properties).get();
        this.tableName = PropertiesUtil.get(Props.TABLE_NAME, properties).get();
        this.replicationFactor = PropertiesUtil.get(Props.REPLICATION_FACTOR, properties).get();
        this.ttl = PropertiesUtil.get(Props.TTL_SECONDS, properties).get();
    }

    public void init() {
        try (CqlSession session = CqlSession.builder().
                withLocalDatacenter(dataCenter).
                addContactEndPoints(
                        Stream.of(nodes).
                                map(x -> new DefaultEndPoint(InetSocketAddressUtil.fromString(x, CassandraDefaults.DEFAULT_CASSANDRA_PORT))).
                                collect(Collectors.toList())).build()) {
            // Create keyspace if it doesn't exist
            session.execute(
                    "CREATE KEYSPACE IF NOT EXISTS " + keyspace +
                            " WITH REPLICATION = { " +
                            "  'class' : 'SimpleStrategy', " +
                            "  'replication_factor' : " + replicationFactor +
                            "};"
            );

            session.execute(
                    "CREATE TABLE IF NOT EXISTS " + keyspace + "." + tableName + " (\n" +
                            "        trace_id uuid,\n" +
                            "        parent_span_id uuid,\n" +
                            "        span_id uuid,\n" +
                            "        payload blob,\n" +
                            "        PRIMARY KEY (\n" +
                            "            (trace_id),\n" +
                            "            parent_span_id,\n" +
                            "            span_id\n" +
                            "        )\n" +
                            "    )\n" +
                            "    WITH\n" +
                            "        CLUSTERING ORDER BY (parent_span_id ASC, span_id ASC)\n" +
                            "        AND comment = 'Tracing span storage'\n" +
                            "        AND default_time_to_live = " + ttl + "\n" +
                            "        AND bloom_filter_fp_chance = 0.01\n" +
                            "        AND compaction = {\n" +
                            "            'class': 'org.apache.cassandra.db.compaction.TimeWindowCompactionStrategy',\n" +
                            "            'compaction_window_unit': 'MINUTES',\n" +
                            "            'compaction_window_size': '90'\n" +
                            "        }\n" +
                            "        AND compression = {\n" +
                            "            'chunk_length_in_kb': '64',\n" +
                            "            'class': 'org.apache.cassandra.io.compress.LZ4Compressor'\n" +
                            "        }\n" +
                            "        AND gc_grace_seconds = 10800\n" +
                            "        AND read_repair_chance = 0.0;"
            );
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

        static final Parameter<Integer> REPLICATION_FACTOR =
                Parameter.integerParameter("replication.factor").
                        withDefault(CassandraDefaults.DEFAULT_REPLICATION_FACTOR).
                        withValidator(IntegerValidators.positive()).
                        build();

        static final Parameter<String> TABLE_NAME =
                Parameter.stringParameter("tableName").
                        withDefault("tracing_spans").
                        build();

        static final Parameter<Integer> TTL_SECONDS =
                Parameter.integerParameter("ttl.seconds").
                        withDefault((int) TimeUnit.DAYS.toSeconds(3)).
                        withValidator(IntegerValidators.positive()).
                        build();
    }
}
