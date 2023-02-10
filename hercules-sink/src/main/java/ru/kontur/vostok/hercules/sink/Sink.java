package ru.kontur.vostok.hercules.sink;

import org.apache.kafka.clients.consumer.CommitFailedException;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.jetbrains.annotations.NotNull;
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
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;
import ru.kontur.vostok.hercules.util.time.TimeSource;
import ru.kontur.vostok.hercules.util.time.Timer;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
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

    private volatile boolean running = true;

    private final ExecutorService executor;
    private final Processor processor;

    private final List<EventFilter> filters;

    private final int batchSize;
    private final long availabilityTimeoutMs;

    private final Pattern pattern;
    private final Consumer<UUID, Event> consumer;

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

        long pollTimeoutMs = PropertiesUtil.get(SinkProps.POLL_TIMEOUT_MS, properties).get();
        this.batchSize = PropertiesUtil.get(SinkProps.BATCH_SIZE, properties).get();
        this.availabilityTimeoutMs = PropertiesUtil.get(SinkProps.AVAILABILITY_TIMEOUT_MS, properties).get();

        String consumerGroupId =
                PropertiesUtil.get(SinkProps.GROUP_ID, properties).
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

        this.consumer = getConsumer(deserializer, consumerProperties, keyDeserializer);

        this.timer = time.timer(pollTimeoutMs);

        this.stat = new Stat(consumerId, time, () -> isRunning() ? consumer.assignment() : Collections.emptySet());
        this.sinkMetrics = sinkMetrics;
    }

    @NotNull
    Consumer<UUID, Event> getConsumer(
            EventDeserializer deserializer,
            Properties consumerProperties,
            UuidDeserializer keyDeserializer
    ) {
        return new KafkaConsumer<>(consumerProperties, keyDeserializer, deserializer);
    }

    /**
     * Start sink.
     */
    @Override
    public final void start() {
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
                            ConsumerRecords<UUID, Event> consumerRecords = poll(timer.toDuration());
                            addToBatch(consumerRecords, events);
                        } while (events.size() < batchSize && !timer.isExpired());

                        stat.setTotalEvents(events.size());

                        if (!events.isEmpty()) {
                            ProcessorResult result = processBatch(events);

                            stat.setProcessorResult(result);
                            sinkMetrics.update(stat);

                            if (result.isSuccess()) {
                                try {
                                    consumer.commitSync();
                                } catch (CommitFailedException ex) {
                                    LOGGER.warn("Commit failed due to rebalancing", ex);
                                }
                            }
                        }
                    }
                } catch (WakeupException ex) {
                    /*
                     * WakeupException is used to terminate consumer operations
                     */
                    return;
                } catch (Exception ex) {
                    LOGGER.error("Unspecified exception has been acquired", ex);
                } finally {
                    unsubscribe();
                }
            }

            processor.awaitAvailability(availabilityTimeoutMs);
        }

        try {
            consumer.close();
        } catch (Exception ex) {
            /* ignore */
        }
    }

    private void addToBatch(ConsumerRecords<UUID, Event> consumerRecords, List<Event> events) {
        Set<TopicPartition> partitions = consumerRecords.partitions();

        stat.markFiltrationStart();
        try {
            for (TopicPartition partition : partitions) {
                List<ConsumerRecord<UUID, Event>> records = consumerRecords.records(partition);
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
    }

    private ProcessorResult processBatch(List<Event> events) {
        stat.markProcessStart();
        try {
            LOGGER.debug("Process events: " + events.size() + ", elapsedTimeMs: " + timer.elapsedTimeMs());
            return processor.process(events);
        } catch (Exception ex) {
            LOGGER.error("Unspecified exception acquired while processing events", ex);
            return ProcessorResult.fail();
        } finally {
            stat.markProcessEnd();
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

    private boolean filter(Event event) {
        for (EventFilter filter : filters) {
            if (!filter.test(event)) {
                return false;
            }
        }
        return true;
    }
}
