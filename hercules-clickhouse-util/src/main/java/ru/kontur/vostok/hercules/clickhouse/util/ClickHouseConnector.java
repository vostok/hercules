package ru.kontur.vostok.hercules.clickhouse.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.util.concurrent.ThreadFactories;
import ru.kontur.vostok.hercules.util.concurrent.Topology;
import ru.kontur.vostok.hercules.util.parameter.Parameter;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;
import ru.kontur.vostok.hercules.util.validation.LongValidators;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Connector manages pool of connections to ClickHouse.
 * <p>
 * If multiple nodes are set, then return another connection from the pool each time using round-robin.
 *
 * @author Gregory Koshelev
 */
public class ClickHouseConnector {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClickHouseConnector.class);

    private static final String URL_TEMPLATE = "jdbc:clickhouse://%s/%s";

    private final Properties connectionProperties;

    private final Topology<ConnectionWrapper> connections;
    private final List<String> failedUrls;

    private final ScheduledExecutorService executor;

    public ClickHouseConnector(Properties properties) {
        this.connectionProperties = PropertiesUtil.ofScope(properties, "properties");

        String db = PropertiesUtil.get(Props.DB, properties).get();
        String[] nodes = PropertiesUtil.get(Props.NODES, properties).get();

        this.failedUrls = new ArrayList<>(nodes.length);

        List<ConnectionWrapper> wrappers = new ArrayList<>(nodes.length);
        for (String node : nodes) {
            String url = String.format(URL_TEMPLATE, node, db);
            Connection conn = tryCreateConn(url);
            if (conn != null) {
                wrappers.add(new ConnectionWrapper(url, conn));
            } else {
                failedUrls.add(url);
            }
        }
        this.connections = new Topology<>(wrappers.toArray(new ConnectionWrapper[0]));

        long validationIntervalMs = PropertiesUtil.get(Props.VALIDATION_INTERVAL_MS, properties).get();

        this.executor = Executors.newSingleThreadScheduledExecutor(
                ThreadFactories.newDaemonNamedThreadFactory("clickhouse-connector-validation"));
        this.executor.scheduleAtFixedRate(this::validate, validationIntervalMs, validationIntervalMs, TimeUnit.MILLISECONDS);
    }

    public Optional<Connection> connection() {
        try {
            return Optional.of(connections.next().connection());
        } catch (Topology.TopologyIsEmptyException ex) {
            return Optional.empty();
        }
    }

    public boolean isConnected() {
        return !connections.isEmpty();
    }

    public void close() {
        executor.shutdown();
        for (ConnectionWrapper wrapper : connections.asList()) {
            tryCloseConn(wrapper.connection());
        }
    }

    /**
     * Validate open connections and re-create failed ones.
     */
    private void validate() {
        for (ConnectionWrapper wrapper : connections.asList()) {
            if (!tryValidateConn(wrapper.connection())) {
                connections.remove(wrapper);
                tryCloseConn(wrapper.connection());
                failedUrls.add(wrapper.url());
            }
        }
        Iterator<String> it = failedUrls.iterator();
        while (it.hasNext()) {
            String url = it.next();
            Connection conn = tryCreateConn(url);
            if (conn != null) {
                connections.add(new ConnectionWrapper(url, conn));
                it.remove();
            }
        }
    }

    private boolean tryValidateConn(Connection conn) {
        if (conn == null) {
            return false;
        }

        try {
            return conn.isValid(5 /* seconds */);
        } catch (SQLException ex) {
            // Should never happened
            LOGGER.error("Cannot validate connection due to exception", ex);
            return false;
        }
    }

    private Connection tryCreateConn(String url) {
        try {
            return DriverManager.getConnection(url, connectionProperties);
        } catch (SQLException ex) {
            LOGGER.error("Cannot create connection due to exception", ex);
            return null;
        } catch (RuntimeException ex) {
            // Got RuntimeException (bug in ClickHouse JDBC driver)
            LOGGER.error("Cannot create connection due to runtime exception", ex);
            return null;
        }
    }

    private void tryCloseConn(Connection conn) {
        if (conn == null) {
            return;
        }

        try {
            conn.close();
        } catch (SQLException ex) {
            LOGGER.error("Cannot close connection due to exception", ex);
        }
    }

    private static class ConnectionWrapper {
        private final String url;
        private final Connection connection;

        ConnectionWrapper(String url, Connection connection) {
            this.url = url;
            this.connection = connection;
        }

        String url() {
            return url;
        }

        Connection connection() {
            return connection;
        }
    }

    private static class Props {
        static final Parameter<String[]> NODES =
                Parameter.stringArrayParameter("nodes").
                        withDefault(new String[]{ClickHouseDefaults.DEFAULT_NODE}).
                        build();

        static final Parameter<String> DB =
                Parameter.stringParameter("db").
                        withDefault(ClickHouseDefaults.DEFAULT_DB).
                        build();

        static final Parameter<Long> VALIDATION_INTERVAL_MS =
                Parameter.longParameter("validationIntervalMs").
                        withDefault(ClickHouseDefaults.DEFAULT_VALIDATION_INTERVAL_MS).
                        withValidator(LongValidators.positive()).
                        build();
    }
}
