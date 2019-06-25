package ru.kontur.vostok.hercules.cassandra.sink;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.AsyncResultSet;
import com.datastax.oss.driver.api.core.cql.BatchStatement;
import com.datastax.oss.driver.api.core.cql.BatchStatementBuilder;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.DefaultBatchType;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.servererrors.QueryValidationException;
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
import ru.kontur.vostok.hercules.util.validation.IntegerValidators;
import ru.kontur.vostok.hercules.util.validation.LongValidators;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author Gregory Koshelev
 */
public abstract class CassandraSender extends Sender {
    private static final Logger LOGGER = LoggerFactory.getLogger(CassandraSender.class);

    private final CassandraConnector cassandraConnector;

    private final long timeoutMs;
    private final int batchSize;

    private volatile PreparedStatement preparedStatement;

    private volatile int batchSizeBytesLimit;
    private volatile int batchSizeBytesMinimum;


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
        batchSize = Props.BATCH_SIZE.extract(properties);
    }

    @Override
    public void start() {
        cassandraConnector.connect();

        CqlSession session = cassandraConnector.session();
        preparedStatement = session.prepare(query());

        batchSizeBytesLimit = cassandraConnector.batchSizeBytesLimit();
        batchSizeBytesMinimum = cassandraConnector.batchSizeBytesMinimum();

        super.start();
    }

    @Override
    protected int send(List<Event> events) throws BackendServiceFailedException {
        CqlSession session = cassandraConnector.session();

        int rejectedEvents = 0;

        List<CompletionStage<AsyncResultSet>> asyncTasks = new ArrayList<>(events.size());
        BatchStatementBuilder batchBuilder = BatchStatement.builder(DefaultBatchType.UNLOGGED);
        int batchSizeBytes = batchSizeBytesMinimum;
        for (Event event : events) {
            Optional<Object[]> converted = convert(event);
            if (!converted.isPresent()) {
                rejectedEvents++;
                continue;
            }

            BoundStatement statement = preparedStatement.bind(converted.get());
            int statementSizeBytes = cassandraConnector.computeInnerBatchStatementSizeBytes(statement);

            if (statementSizeBytes + batchSizeBytesMinimum >= batchSizeBytesLimit) {
                asyncTasks.add(session.executeAsync(statement));
                continue;
            }

            if (statementSizeBytes + batchSizeBytes > batchSizeBytesLimit || batchBuilder.getStatementsCount() >= batchSize) {
                asyncTasks.add(session.executeAsync(batchBuilder.build()));

                batchBuilder = BatchStatement.builder(DefaultBatchType.UNLOGGED);
                batchSizeBytes = batchSizeBytesMinimum;
            }

            batchBuilder.addStatement(statement);
            batchSizeBytes += statementSizeBytes;
        }

        if (batchBuilder.getStatementsCount() > 0) {
            asyncTasks.add(session.executeAsync(batchBuilder.build()));
        }

        long elapsedTimeMs = 0L;
        long remainingTimeMs = timeoutMs;
        final long startedAtMs = System.currentTimeMillis();

        for (CompletionStage<AsyncResultSet> asyncTask : asyncTasks) {
            try {
                asyncTask.toCompletableFuture().get(remainingTimeMs, TimeUnit.MILLISECONDS);
            } catch (TimeoutException | InterruptedException ex) {
                throw new BackendServiceFailedException(ex);
            } catch (ExecutionException ex) {
                Throwable cause = ex.getCause();
                if (cause instanceof QueryValidationException) {
                    LOGGER.warn("Event dropped due to exception", cause);
                    rejectedEvents++;
                } else {
                    throw new BackendServiceFailedException(cause);
                }
            } catch (RuntimeException ex) {
                LOGGER.warn("Event dropped due to exception", ex);
                rejectedEvents++;
            }

            elapsedTimeMs = StopwatchUtil.elapsedTime(startedAtMs);
            remainingTimeMs = StopwatchUtil.remainingTimeOrZero(timeoutMs, elapsedTimeMs);
        }

        return events.size() - rejectedEvents;
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
     * Convert event to array of objects is acceptable in {@link PreparedStatement#bind(Object...)}
     *
     * @param event the event to convert
     * @return optional array of objects is acceptable in {@link PreparedStatement#bind(Object...)}
     * or {@link Optional#empty()} if event is invalid
     */
    protected abstract Optional<Object[]> convert(Event event);

    private static class Props {
        static final PropertyDescription<Long> SEND_TIMEOUT_MS =
                PropertyDescriptions.longProperty("sendTimeoutMs").
                        withDefaultValue(60_000L).
                        withValidator(LongValidators.positive()).
                        build();

        static final PropertyDescription<Integer> BATCH_SIZE =
                PropertyDescriptions.integerProperty("batchSize").
                        withDefaultValue(10).
                        withValidator(IntegerValidators.positive()).
                        build();
    }
}
