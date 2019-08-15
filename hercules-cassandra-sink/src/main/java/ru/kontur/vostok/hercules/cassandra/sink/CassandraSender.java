package ru.kontur.vostok.hercules.cassandra.sink;

import com.datastax.oss.driver.api.core.CqlIdentifier;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.AsyncResultSet;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.metadata.Node;
import com.datastax.oss.driver.api.core.metadata.TokenMap;
import com.datastax.oss.driver.api.core.servererrors.QueryValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.cassandra.util.CassandraConnector;
import ru.kontur.vostok.hercules.configuration.Scopes;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.kafka.util.processing.BackendServiceFailedException;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.sink.Sender;
import ru.kontur.vostok.hercules.util.properties.PropertyDescription;
import ru.kontur.vostok.hercules.util.properties.PropertyDescriptions;
import ru.kontur.vostok.hercules.util.time.StopwatchUtil;
import ru.kontur.vostok.hercules.util.validation.IntegerValidators;
import ru.kontur.vostok.hercules.util.validation.LongValidators;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Base Cassandra Sender.
 * <p>
 * Sends events with batches.
 *
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
        CqlIdentifier keyspace = session.getKeyspace().get();
        Optional<TokenMap> tokenMap = session.getMetadata().getTokenMap();

        int rejectedEvents = 0;


        List<CompletionStage<AsyncResultSet>> asyncTasks = new ArrayList<>(events.size());
        Map<Set<Node>, BatchBuilder> batchBuilders = new HashMap<>();
        for (Event event : events) {
            Optional<Object[]> converted = convert(event);
            if (!converted.isPresent()) {
                rejectedEvents++;
                continue;
            }

            BoundStatement statement = preparedStatement.bind(converted.get());
            int statementSizeBytes = cassandraConnector.computeInnerBatchStatementSizeBytes(statement);

            // Send statement without batch if it's too large
            if (statementSizeBytes + batchSizeBytesMinimum >= batchSizeBytesLimit) {
                asyncTasks.add(session.executeAsync(statement));
                continue;
            }

            // Otherwise, implement node awareness
            ByteBuffer routingKey = statement.getRoutingKey();
            Set<Node> nodes =
                    (routingKey != null && tokenMap.isPresent())
                            ? tokenMap.get().getReplicas(keyspace, routingKey)
                            : Collections.emptySet();
            BatchBuilder batchBuilder = batchBuilders.computeIfAbsent(
                    nodes,
                    k -> new BatchBuilder(cassandraConnector));

            if (statementSizeBytes + batchBuilder.getBatchSizeBytes() > batchSizeBytesLimit || batchBuilder.getStatementsCount() >= batchSize) {
                asyncTasks.add(session.executeAsync(batchBuilder.build()));
                batchBuilders.put(nodes, batchBuilder = new BatchBuilder(cassandraConnector));
            }

            batchBuilder.addStatement(statement, statementSizeBytes);
        }

        for (BatchBuilder batchBuilder : batchBuilders.values()) {
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
        boolean stopped = false;
        try {
            stopped = super.stop(timeout, unit);
        } finally {
            cassandraConnector.close();
        }
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
