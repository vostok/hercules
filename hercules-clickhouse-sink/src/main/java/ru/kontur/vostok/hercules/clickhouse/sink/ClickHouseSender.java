package ru.kontur.vostok.hercules.clickhouse.sink;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.clickhouse.util.ClickHouseConnector;
import ru.kontur.vostok.hercules.health.AutoMetricStopwatch;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.health.Timer;
import ru.kontur.vostok.hercules.kafka.util.processing.BackendServiceFailedException;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.sink.ProcessorStatus;
import ru.kontur.vostok.hercules.sink.Sender;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * @author Gregory Koshelev
 */
public abstract class ClickHouseSender extends Sender {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClickHouseSender.class);

    private final ClickHouseConnector connector;
    private volatile Connection connection;

    private final Timer preparationTimeMsTimer;
    private final Timer insertionTimeMsTimer;

    public ClickHouseSender(Properties properties, MetricsCollector metricsCollector) {
        super(properties, metricsCollector);

        Properties clickhouseProperties = PropertiesUtil.ofScope(properties, "clickhouse");
        this.connector = new ClickHouseConnector(clickhouseProperties);

        this.preparationTimeMsTimer = metricsCollector.timer("preparationTimeMs");
        this.insertionTimeMsTimer = metricsCollector.timer("insertionTimeMs");
    }

    @Override
    protected int send(List<Event> events) throws BackendServiceFailedException {
        Connection conn = getOrUpdateConnection();

        try (PreparedStatement preparedStatement = conn.prepareStatement(query())) {
            try (AutoMetricStopwatch ignored = new AutoMetricStopwatch(preparationTimeMsTimer, TimeUnit.MILLISECONDS)) {
                for (Event event : events) {
                    if (bind(preparedStatement, event)) {
                        preparedStatement.addBatch();
                    }
                }
            }

            try (AutoMetricStopwatch ignored = new AutoMetricStopwatch(insertionTimeMsTimer, TimeUnit.MILLISECONDS)) {
                return preparedStatement.executeBatch().length;
            }
        } catch (SQLException ex) {
            invalidateConnection();
            throw new BackendServiceFailedException(ex);
        } catch (RuntimeException ex) {
            LOGGER.error("Unexpected error has been acquired", ex);
            invalidateConnection();
            throw new BackendServiceFailedException(ex);
        }
    }

    @Override
    protected ProcessorStatus ping() {
        return connector.connection().isPresent() ? ProcessorStatus.AVAILABLE : ProcessorStatus.UNAVAILABLE;
    }

    @Override
    public boolean stop(long timeout, TimeUnit unit) {
        boolean stopped;
        try {
            stopped = super.stop(timeout, unit);
        } finally {
            connection = null;
            connector.close();
        }
        return stopped;
    }

    private Connection getOrUpdateConnection() throws BackendServiceFailedException {
        Connection conn = connection;
        return (conn != null) ? conn : (connection = connector.connection().orElseThrow(BackendServiceFailedException::new));
    }

    private void invalidateConnection() {
        connection = null;
    }

    /**
     * INSERT query.
     * <p>
     * Query must be like {@code INSERT INTO [db.]table [(column1, column2, column3)] VALUES (?, ?, ?)}.
     *
     * @return INSERT query
     */
    protected abstract String query();

    /**
     * Bind the event to the prepared statement parameter list.
     *
     * @param preparedStatement the prepared statement
     * @param event             the event to bind
     * @return {@code true} if bind the event, otherwise return {@code false}
     * @throws SQLException in case of invalid {@link PreparedStatement}. Also, some misconfiguration is possible.
     */
    protected abstract boolean bind(PreparedStatement preparedStatement, Event event) throws SQLException;
}
