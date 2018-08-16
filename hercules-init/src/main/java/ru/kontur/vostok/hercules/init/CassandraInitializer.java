package ru.kontur.vostok.hercules.init;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import ru.kontur.vostok.hercules.cassandra.util.CassandraDefaults;
import ru.kontur.vostok.hercules.util.properties.PropertiesExtractor;

import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * @author Gregory Koshelev
 */
public class CassandraInitializer {
    private final Properties cassandraProperties;

    public CassandraInitializer(Properties properties) {
        this.cassandraProperties = properties;
    }

    public void init() {
        List<String> nodes = PropertiesExtractor.toList(cassandraProperties, "nodes");
        if (nodes.isEmpty()) {
            nodes = Collections.singletonList(CassandraDefaults.DEFAULT_CASSANDRA_ADDRESS);
        }

        final short replicationFactor = PropertiesExtractor.getShort(cassandraProperties, "replication.factor", CassandraDefaults.DEFAULT_REPLICATION_FACTOR);
        final int port = PropertiesExtractor.get(cassandraProperties, "port", CassandraDefaults.DEFAULT_CASSANDRA_PORT);
        final String keyspace = cassandraProperties.getProperty("keyspace", CassandraDefaults.DEFAULT_KEYSPACE);

        Cluster.Builder builder = Cluster.builder().withPort(port).withoutJMXReporting();

        for (String node : nodes) {
            builder.addContactPoint(node);
        }

        try (Cluster cluster = builder.build(); Session session = cluster.connect()) {
            // Create keyspace if it doesn't exist
            session.execute("CREATE KEYSPACE IF NOT EXISTS " + keyspace + " WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : " + replicationFactor + " };");
        }
    }
}
