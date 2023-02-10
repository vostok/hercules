package ru.kontur.vostok.hercules.clickhouse.sink;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.clickhouse.util.ClickHouseConnector;
import ru.kontur.vostok.hercules.configuration.Scopes;
import ru.kontur.vostok.hercules.health.AutoMetricStopwatch;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.health.Timer;
import ru.kontur.vostok.hercules.kafka.util.processing.BackendServiceFailedException;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.sink.ProcessorStatus;
import ru.kontur.vostok.hercules.sink.parallel.sender.NoPrepareParallelSender;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;
import ru.yandex.clickhouse.ClickHouseConnection;
import ru.yandex.clickhouse.ClickHouseStatement;
import ru.yandex.clickhouse.domain.ClickHouseFormat;
import ru.yandex.clickhouse.util.ClickHouseRowBinaryStream;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Base ClickHouse sender.
 * <p>
 * Events are converted to satisfy SQL-query {@link #query()} in according to table schema in DB.
 * Sender inserts events by batches to ClickHouse using binary format.
 * Thus, accurate implementation of {@link #write(ClickHouseRowBinaryStream, Event)} method is required.
 *
 * @author Gregory Koshelev
 */
public abstract class ClickHouseSender extends NoPrepareParallelSender {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClickHouseSender.class);

    private final ClickHouseConnector connector;

    private final Timer processingTimeMsTimer;

    public ClickHouseSender(Properties properties, MetricsCollector metricsCollector) {
        super(properties, metricsCollector);

        Properties clickhouseProperties = PropertiesUtil.ofScope(properties, Scopes.CLICKHOUSE);
        this.connector = new ClickHouseConnector(clickhouseProperties);

        this.processingTimeMsTimer = metricsCollector.timer("processingTimeMs");
    }

    @Override
    protected int send(List<Event> events) throws BackendServiceFailedException {
        ClickHouseConnection conn = getConnection();

        final AtomicInteger count = new AtomicInteger(0);
        try (ClickHouseStatement statement = conn.createStatement();
             AutoMetricStopwatch ignored = new AutoMetricStopwatch(processingTimeMsTimer, TimeUnit.MILLISECONDS)) {
            statement.write().send(
                    query(),
                    stream -> {
                        for (Event event : events) {
                            if (write(stream, event)) {
                                count.incrementAndGet();
                            }
                        }
                    },
                    ClickHouseFormat.RowBinary);
        } catch (SQLException ex) {
            throw new BackendServiceFailedException(ex);
        } catch (RuntimeException ex) {
            LOGGER.error("Unexpected error has been acquired", ex);
            throw new BackendServiceFailedException(ex);
        }

        return count.get();
    }

    @Override
    protected ProcessorStatus ping() {
        return connector.isConnected() ? ProcessorStatus.AVAILABLE : ProcessorStatus.UNAVAILABLE;
    }

    @Override
    public boolean stop(long timeout, TimeUnit unit) {
        boolean stopped;
        try {
            stopped = super.stop(timeout, unit);
        } finally {
            connector.close();
        }
        return stopped;
    }

    private ClickHouseConnection getConnection() throws BackendServiceFailedException {
        return (ClickHouseConnection) connector.connection().orElseThrow(BackendServiceFailedException::new);
    }

    /**
     * INSERT query.
     * <p>
     * Query must be like {@code INSERT INTO [db.]table [(column1, column2, column3)]}.
     *
     * @return INSERT query
     */
    protected abstract String query();

    /**
     * Write the event to row binary stream.
     *
     * @param event the event
     * @return {@code true} if the event has been written, otherwise {@code false}
     */
    protected abstract boolean write(ClickHouseRowBinaryStream stream, Event event) throws IOException;
}
