package ru.kontur.vostok.hercules.clickhouse.util;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.util.parameter.Parameter;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;
import ru.yandex.clickhouse.BalancedClickhouseDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Gregory Koshelev
 */
public class ClickHouseConnector {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClickHouseConnector.class);

    private final BalancedClickhouseDataSource dataSource;

    private AtomicReference<Connection> connection = new AtomicReference<>();

    public ClickHouseConnector(Properties properties) {
        this.dataSource =
                new BalancedClickhouseDataSource(
                        PropertiesUtil.get(Props.URL, properties).get(),
                        PropertiesUtil.ofScope(properties, "properties"));
    }

    private static class Props {
        static final Parameter<String> URL =
                Parameter.stringParameter("url").
                        withDefault(ClickHouseDefaults.DEFAULT_URL).
                        build();
    }

    public Optional<Connection> connection() {
        Connection conn = connection.get();

        if (tryValidateConn(conn)) {
            return Optional.of(conn);
        }

        Connection newConn = tryCreateConn();
        if (newConn == null) {
            return Optional.empty();
        }

        if (connection.compareAndSet(conn, newConn)) {
            // Try to close invalid connection and return the new one
            tryCloseConn(conn);
            return Optional.of(newConn);
        } else {
            // Suddenly, the new connection has been already created a moment ago. Thus, use this one.
            tryCloseConn(newConn);
            return Optional.of(connection.get());
        }
    }

    public void close() {
        Connection conn = connection.get();
        tryCloseConn(conn);
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

    private Connection tryCreateConn() {
        try {
            return dataSource.getConnection();
        } catch (SQLException ex) {
            LOGGER.error("Cannot create connection due to exception", ex);
            return null;
        }
    }

    private void tryCloseConn(@Nullable Connection conn) {
        if (conn == null) {
            return;
        }

        try {
            conn.close();
        } catch (SQLException ex) {
            LOGGER.error("Cannot close connection due to exception", ex);
        }
    }
}
