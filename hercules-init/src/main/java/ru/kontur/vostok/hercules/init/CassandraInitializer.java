package ru.kontur.vostok.hercules.init;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import ru.kontur.vostok.hercules.cassandra.util.CassandraDefaults;
import ru.kontur.vostok.hercules.util.properties.PropertyDescription;
import ru.kontur.vostok.hercules.util.properties.PropertyDescriptions;
import ru.kontur.vostok.hercules.util.validation.Validators;

import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * @author Gregory Koshelev
 */
public class CassandraInitializer {

    private static class Props {
        static final PropertyDescription<List<String>> NODES = PropertyDescriptions
                .listOfStringsProperty("nodes")
                .withDefaultValue(Collections.singletonList(CassandraDefaults.DEFAULT_CASSANDRA_ADDRESS))
                .build();

        static final PropertyDescription<Integer> PORT = PropertyDescriptions
                .integerProperty("port")
                .withDefaultValue(CassandraDefaults.DEFAULT_CASSANDRA_PORT)
                .withValidator(Validators.portValidator())
                .build();

        static final PropertyDescription<String> KEYSPACE = PropertyDescriptions
                .stringProperty("keyspace")
                .withDefaultValue(CassandraDefaults.DEFAULT_KEYSPACE)
                .build();

        static final PropertyDescription<Short> REPLICATION_FACTOR = PropertyDescriptions
                .shortProperty("replication.factor")
                .withDefaultValue(CassandraDefaults.DEFAULT_REPLICATION_FACTOR)
                .withValidator(Validators.greaterThan((short) 0))
                .build();
    }

    private final List<String> nodes;
    private final int port;
    private final String keyspace;
    private final short replicationFactor;

    public CassandraInitializer(Properties properties) {
        this.nodes = Props.NODES.extract(properties);
        this.port = Props.PORT.extract(properties);
        this.keyspace = Props.KEYSPACE.extract(properties);
        this.replicationFactor = Props.REPLICATION_FACTOR.extract(properties);
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
        }
    }
}
