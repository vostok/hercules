package ru.kontur.vostok.hercules.cassandra.sink;

import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSetFuture;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.exceptions.NoHostAvailableException;
import com.datastax.driver.core.exceptions.QueryExecutionException;
import com.datastax.driver.core.exceptions.QueryValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.cassandra.util.CassandraConnector;
import ru.kontur.vostok.hercules.configuration.Scopes;
import ru.kontur.vostok.hercules.configuration.util.PropertiesUtil;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.kafka.util.processing.BackendServiceFailedException;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.sink.Sender;
import ru.kontur.vostok.hercules.util.properties.PropertyDescription;
import ru.kontur.vostok.hercules.util.properties.PropertyDescriptions;
import ru.kontur.vostok.hercules.util.time.StopwatchUtil;
import ru.kontur.vostok.hercules.util.validation.LongValidators;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author Gregory Koshelev
 */
public abstract class CassandraSender extends Sender {
    private static final Logger LOGGER = LoggerFactory.getLogger(CassandraSender.class);

    private final CassandraConnector cassandraConnector;

    private final long timeoutMs;

    private volatile PreparedStatement preparedStatement;

    /**
     * Base Cassandra Sender.
     *
     * @param properties       sender's properties.
     * @param metricsCollector metrics collector
     */
    public CassandraSender(Properties properties, MetricsCollector metricsCollector) {
        super(properties, metricsCollector);

        Properties cassandraProperties = PropertiesUtil.ofScope(properties, Scopes.CASSANDRA);
        cassandraConnector = new CassandraConnector(cassandraProperties);

        timeoutMs = Props.SEND_TIMEOUT_MS.extract(properties);
    }

    @Override
    public void start() {
        cassandraConnector.connect();

        Session session = cassandraConnector.session();
        preparedStatement = session.prepare(query());

        super.start();
    }

    @Override
    protected int send(List<Event> events) throws BackendServiceFailedException {
        Session session = cassandraConnector.session();

        int droppedEvents = 0;

        List<ResultSetFuture> futures = new ArrayList<>(events.size());
        for (Event event : events) {
            Optional<Object[]> converted = convert(event);
            if (!converted.isPresent()) {
                droppedEvents++;
                continue;
            }

            futures.add(session.executeAsync(preparedStatement.bind()));
        }

        long elapsedTimeMs = 0L;
        long remainingTimeMs = timeoutMs;
        final long startedAtMs = System.currentTimeMillis();

        for (ResultSetFuture future : futures) {
            try {
                future.getUninterruptibly(remainingTimeMs, TimeUnit.MILLISECONDS);
            } catch (TimeoutException | NoHostAvailableException | QueryExecutionException ex) {
                throw new BackendServiceFailedException(ex);
            } catch (QueryValidationException ex) {
                LOGGER.warn("Event dropped due to exception", ex);
                droppedEvents++;
            }

            elapsedTimeMs = StopwatchUtil.elapsedTime(startedAtMs);
            remainingTimeMs = StopwatchUtil.remainingTimeOrZero(timeoutMs, elapsedTimeMs);
        }

        return events.size() - droppedEvents;
    }

    @Override
    public boolean stop(long timeout, TimeUnit unit) {
        boolean stopped = super.stop(timeout, unit);
        cassandraConnector.close();
        return stopped;
    }

    /**
     * Query template to build {@link PreparedStatement}.
     *
     * @return query string
     */
    protected abstract String query();

    /**
     * Convert event to array of objects is acceptable in {@link com.datastax.driver.core.PreparedStatement#bind(Object...)}
     *
     * @param event the event to convert
     * @return optional array of objects is acceptable in {@link com.datastax.driver.core.PreparedStatement#bind(Object...)}
     * or {@link Optional#empty()} if event is invalid
     */
    protected abstract Optional<Object[]> convert(Event event);

    private static class Props {
        static final PropertyDescription<Long> SEND_TIMEOUT_MS =
                PropertyDescriptions.longProperty("sendTimeoutMs").
                        withDefaultValue(60_000L).
                        withValidator(LongValidators.positive()).
                        build();
    }
}
