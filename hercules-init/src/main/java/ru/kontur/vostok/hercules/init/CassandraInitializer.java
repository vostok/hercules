package ru.kontur.vostok.hercules.init;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.internal.core.metadata.DefaultEndPoint;
import ru.kontur.vostok.hercules.cassandra.util.CassandraDefaults;
import ru.kontur.vostok.hercules.util.net.InetSocketAddressUtil;
import ru.kontur.vostok.hercules.util.parameter.Parameter;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;
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
        this.dataCenter = PropertiesUtil.get(Props.DATA_CENTER, properties).get();
        this.nodes = PropertiesUtil.get(Props.NODES, properties).get();
        this.keyspace = PropertiesUtil.get(Props.KEYSPACE, properties).get();
        this.replicationFactor = PropertiesUtil.get(Props.REPLICATION_FACTOR, properties).get();
    }

    public void init() {
        //TODO Use CassandraConnector with CassandraAuthProvider
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
    }
}
