package ru.kontur.vostok.hercules.init;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import ru.kontur.vostok.hercules.cassandra.util.CassandraDefaults;
import ru.kontur.vostok.hercules.util.properties.PropertyDescription;
import ru.kontur.vostok.hercules.util.properties.PropertyDescriptions;
import ru.kontur.vostok.hercules.util.validation.Validators;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * @author Gregory Koshelev
 */
public class CassandraTracingInitializer {
    private final String[] nodes;
    private final int port;
    private final String keyspace;
    private final String tableName;
    private final short replicationFactor;
    private final int ttl;

    public CassandraTracingInitializer(Properties properties) {
        this.nodes = Props.NODES.extract(properties);
        this.port = Props.PORT.extract(properties);
        this.keyspace = Props.KEYSPACE.extract(properties);
        this.tableName = Props.TABLE_NAME.extract(properties);
        this.replicationFactor = Props.REPLICATION_FACTOR.extract(properties);
        this.ttl = Props.TTL_SECONDS.extract(properties);
    }

    public void init() {
        Cluster.Builder builder = Cluster.builder().withPort(port).withoutJMXReporting();

        for (String node : nodes) {
            builder.addContactPoint(node);
        }

        try (Cluster cluster = builder.build(); Session session = cluster.connect()) {
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
        static final PropertyDescription<String[]> NODES =
                PropertyDescriptions.arrayOfStringsProperty("nodes").
                        withDefaultValue(new String[]{CassandraDefaults.DEFAULT_CASSANDRA_ADDRESS}).
                        build();

        static final PropertyDescription<Integer> PORT =
                PropertyDescriptions.integerProperty("port").
                        withDefaultValue(CassandraDefaults.DEFAULT_CASSANDRA_PORT).
                        withValidator(Validators.portValidator()).
                        build();

        static final PropertyDescription<String> KEYSPACE =
                PropertyDescriptions.stringProperty("keyspace").
                        withDefaultValue(CassandraDefaults.DEFAULT_KEYSPACE).
                        build();

        static final PropertyDescription<String> TABLE_NAME =
                PropertyDescriptions.stringProperty("tableName").
                        withDefaultValue("tracing_spans").
                        build();

        static final PropertyDescription<Short> REPLICATION_FACTOR =
                PropertyDescriptions.shortProperty("replication.factor").
                        withDefaultValue(CassandraDefaults.DEFAULT_REPLICATION_FACTOR).
                        withValidator(Validators.greaterThan((short) 0)).
                        build();

        static final PropertyDescription<Integer> TTL_SECONDS =
                PropertyDescriptions.integerProperty("ttl.seconds").
                        withDefaultValue((int) TimeUnit.DAYS.toSeconds(3)).
                        withValidator(Validators.greaterThan(0)).
                        build();
    }
}
