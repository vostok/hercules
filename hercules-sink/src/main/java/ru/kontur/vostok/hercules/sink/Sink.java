package ru.kontur.vostok.hercules.sink;

import org.apache.kafka.clients.consumer.CommitFailedException;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.application.Application;
import ru.kontur.vostok.hercules.configuration.Scopes;
import ru.kontur.vostok.hercules.kafka.util.KafkaConfigs;
import ru.kontur.vostok.hercules.kafka.util.consumer.Subscription;
import ru.kontur.vostok.hercules.kafka.util.serialization.EventDeserializer;
import ru.kontur.vostok.hercules.kafka.util.serialization.UuidDeserializer;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.sink.filter.EventFilter;
import ru.kontur.vostok.hercules.sink.metrics.SinkMetrics;
import ru.kontur.vostok.hercules.sink.metrics.Stat;
import ru.kontur.vostok.hercules.util.lifecycle.Lifecycle;
import ru.kontur.vostok.hercules.util.parameter.Parameter;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;
import ru.kontur.vostok.hercules.util.time.TimeSource;
import ru.kontur.vostok.hercules.util.time.Timer;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

/**
 * @author Gregory Koshelev
 */
public class Sink implements Lifecycle {
    private static final Logger LOGGER = LoggerFactory.getLogger(Sink.class);
    private static final AtomicInteger CONSUMER_CLIENT_ID_SEQUENCE = new AtomicInteger(1);

    private volatile boolean running = false;

    private final ExecutorService executor;
    private final Processor processor;

    private final List<EventFilter> filters;

    private final int batchSize;
    private final long availabilityTimeoutMs;

    private final Pattern pattern;
    private final KafkaConsumer<UUID, Event> consumer;

    private final Timer timer;

    private final Stat stat;
    private final SinkMetrics sinkMetrics;

    public Sink(
            ExecutorService executor,
            String applicationId,
            Properties properties,
            Processor processor,
            Subscription subscription,
            EventDeserializer deserializer,
            SinkMetrics sinkMetrics) {
        this(executor, applicationId, properties, processor, subscription, deserializer, sinkMetrics, TimeSource.SYSTEM);
    }

    Sink(
            ExecutorService executor,
            String applicationId,
            Properties properties,
            Processor processor,
            Subscription subscription,
            EventDeserializer deserializer,
            SinkMetrics sinkMetrics,
            TimeSource time) {
        this.executor = executor;
        this.processor = processor;

        this.filters = EventFilter.from(PropertiesUtil.ofScope(properties, "filter"));

        long pollTimeoutMs = PropertiesUtil.get(Props.POLL_TIMEOUT_MS, properties).get();
        this.batchSize = PropertiesUtil.get(Props.BATCH_SIZE, properties).get();
        this.availabilityTimeoutMs = PropertiesUtil.get(Props.AVAILABILITY_TIMEOUT_MS, properties).get();

        String consumerGroupId =
                PropertiesUtil.get(Props.GROUP_ID, properties).
                        orEmpty(subscription.toGroupId(applicationId));
        Application.context().put(SinkContext.GROUP_ID, consumerGroupId);

        this.pattern = subscription.toPattern();
        Application.context().put(SinkContext.SUBSCRIPTION, subscription.toString());

        Properties consumerProperties = PropertiesUtil.ofScope(properties, Scopes.CONSUMER);
        consumerProperties.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroupId);
        String consumerId = CONSUMER_CLIENT_ID_SEQUENCE.getAndIncrement() + "-" + Application.context().getInstanceId();
        consumerProperties.put(ConsumerConfig.CLIENT_ID_CONFIG, consumerId);
        consumerProperties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        consumerProperties.putIfAbsent(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, batchSize);
        consumerProperties.put(KafkaConfigs.METRICS_COLLECTOR_INSTANCE_CONFIG, sinkMetrics.getMetricsCollector());

        UuidDeserializer keyDeserializer = new UuidDeserializer();

        this.consumer = new KafkaConsumer<>(consumerProperties, keyDeserializer, deserializer);

        this.timer = time.timer(pollTimeoutMs);

        this.stat = new Stat(consumerId, time, consumer::assignment);
        this.sinkMetrics = sinkMetrics;
    }

    /**
     * Start sink.
     */
    @Override
    public final void start() {
        running = true;

        executor.execute(this::run);
    }

    /**
     * Stop Sink.
     */
    @Override
    public final boolean stop(long timeout, TimeUnit unit) {
        running = false;

        try {
            consumer.wakeup();
        } catch (Exception ex) {
            /* ignore */
        }

        try {
            consumer.close();
        } catch (Exception ex) {
            /* ignore */
        }

        postStop();
        return true;
    }

    /**
     * Check Sink running status.
     *
     * @return {@code true} if Sink is running and {@code false} if Sink is stopping
     */
    public final boolean isRunning() {
        return running;
    }

    /**
     * Main Sink logic. Sink poll events from Kafka and processes them using {@link Processor} if possible.
     * <p>
     * Sink awaits availability of {@link Processor}. Also, it controls {@link #isRunning()} during operations.
     */
    public final void run() {
        List<Event> events = new ArrayList<>(batchSize * 2);

        while (isRunning()) {
            if (processor.isAvailable()) {
                try {

                    subscribe();

                    while (processor.isAvailable()) {
                        events.clear();
                        timer.reset();
                        stat.reset();

                        do {
                            ConsumerRecords<UUID, Event> pollResult;
                            try {
                                pollResult = poll(timer.toDuration());
                            } catch (WakeupException ex) {
                                /*
                                 * WakeupException is used to terminate polling
                                 */
                                return;
                            }

                            Set<TopicPartition> partitions = pollResult.partitions();

                            stat.markFiltrationStart();
                            try {
                                for (TopicPartition partition : partitions) {
                                    List<ConsumerRecord<UUID, Event>> records = pollResult.records(partition);
                                    for (ConsumerRecord<UUID, Event> record : records) {
                                        Event event = record.value();
                                        if (event == null) {// Received non-deserializable data, should be ignored
                                            stat.incrementDroppedEvents();
                                            continue;
                                        }
                                        if (!filter(event)) {
                                            stat.incrementFilteredEvents();
                                            continue;
                                        }
                                        events.add(event);
                                        stat.incrementTotalEventsPerPartition(partition);
                                        stat.incrementBatchSizePerPartition(partition, event.sizeOf());
                                    }
                                }
                            } catch (Exception ex) {
                                LOGGER.error("Unspecified exception acquired while filtration", ex);
                            } finally {
                                stat.markFiltrationEnd();
                            }

                        } while (events.size() < batchSize && !timer.isExpired());

                        stat.setTotalEvents(events.size());

                        ProcessorResult result;
                        stat.markProcessStart();
                        try {
                            result = processor.process(events);
                        } catch (Exception ex) {
                            LOGGER.error("Unspecified exception acquired while processing events", ex);
                            result = ProcessorResult.fail();
                        } finally {
                            stat.markProcessEnd();
                        }

                        stat.setProcessorResult(result);
                        sinkMetrics.update(stat);

                        if (result.isSuccess()) {
                            try {
                                commit();
                            } catch (CommitFailedException ex) {
                                LOGGER.warn("Commit failed due to rebalancing", ex);
                            }
                        }
                    }
                } catch (Exception ex) {
                    LOGGER.error("Unspecified exception has been acquired", ex);
                } finally {
                    unsubscribe();
                }
            }

            processor.awaitAvailability(availabilityTimeoutMs);
        }
    }

    /**
     * Perform additional stop operations when Event consuming was terminated.
     */
    protected void postStop() {

    }

    /**
     * Subscribe Sink. Should be called before polling
     */
    protected final void subscribe() {
        consumer.subscribe(pattern);
    }

    /**
     * Unsubscribe Sink. Should be called if Sink cannot process Events.
     */
    protected final void unsubscribe() {
        LOGGER.debug("Sink unsubscribe if any");
        try {
            consumer.unsubscribe();
        } catch (Exception ex) {
            /* ignore */
        }
    }

    /**
     * Poll Events from Kafka. Should be called when Sink subscribed.
     *
     * @return polled Events
     * @throws WakeupException if poll terminated due to shutdown
     */
    protected final ConsumerRecords<UUID, Event> poll(Duration timeout) throws Exception {
        stat.markPollStart();
        try {
            return consumer.poll(timeout);
        } finally {
            stat.markPollEnd();
        }
    }

    protected final void commit() {
        consumer.commitSync();
    }

    private boolean filter(Event event) {
        for (EventFilter filter : filters) {
            if (!filter.test(event)) {
                return false;
            }
        }
        return true;
    }

    private static class Props {
        static final Parameter<Long> POLL_TIMEOUT_MS =
                Parameter.longParameter("pollTimeoutMs").
                        withDefault(6_000L).
                        build();

        static final Parameter<Integer> BATCH_SIZE =
                Parameter.integerParameter("batchSize").
                        withDefault(1000).
                        build();

        static final Parameter<String> GROUP_ID =
                Parameter.stringParameter("groupId").
                        build();

        static final Parameter<Long> AVAILABILITY_TIMEOUT_MS =
                Parameter.longParameter("availabilityTimeoutMs").
                        withDefault(2_000L).
                        build();
    }
}
