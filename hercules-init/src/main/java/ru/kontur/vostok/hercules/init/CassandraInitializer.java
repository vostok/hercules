package ru.kontur.vostok.hercules.init;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.internal.core.metadata.DefaultEndPoint;
import ru.kontur.vostok.hercules.cassandra.util.CassandraDefaults;
import ru.kontur.vostok.hercules.util.net.InetSocketAddressUtil;
import ru.kontur.vostok.hercules.util.properties.PropertyDescription;
import ru.kontur.vostok.hercules.util.properties.PropertyDescriptions;
import ru.kontur.vostok.hercules.util.validation.IntegerValidators;

import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Gregory Koshelev
 */
public class CassandraInitializer {
    private final String dataCenter;
    private final String[] nodes;
    private final String keyspace;
    private final int replicationFactor;

    public CassandraInitializer(Properties properties) {
        this.dataCenter = Props.DATA_CENTER.extract(properties);
        this.nodes = Props.NODES.extract(properties);
        this.keyspace = Props.KEYSPACE.extract(properties);
        this.replicationFactor = Props.REPLICATION_FACTOR.extract(properties);
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

        static final PropertyDescription<Integer> REPLICATION_FACTOR =
                PropertyDescriptions.integerProperty("replication.factor").
                        withDefaultValue(CassandraDefaults.DEFAULT_REPLICATION_FACTOR).
                        withValidator(IntegerValidators.positive()).
                        build();
    }
}
