package ru.kontur.vostok.hercules.sink;

import org.apache.kafka.clients.consumer.CommitFailedException;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.configuration.Scopes;
import ru.kontur.vostok.hercules.health.Meter;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.kafka.util.KafkaConfigs;
import ru.kontur.vostok.hercules.kafka.util.serialization.EventDeserializer;
import ru.kontur.vostok.hercules.kafka.util.serialization.UuidDeserializer;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.sink.filter.EventFilter;
import ru.kontur.vostok.hercules.util.lifecycle.Lifecycle;
import ru.kontur.vostok.hercules.util.parameter.Parameter;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;
import ru.kontur.vostok.hercules.util.time.TimeSource;
import ru.kontur.vostok.hercules.util.time.Timer;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * @author Gregory Koshelev
 */
public class Sink implements Lifecycle {
    private static final Logger LOGGER = LoggerFactory.getLogger(Sink.class);

    private volatile boolean running = false;

    private final ExecutorService executor;
    private final Processor processor;

    private final List<EventFilter> filters;

    private final long pollTimeoutMs;
    private final int batchSize;
    private final long availabilityTimeoutMs;

    private final Pattern pattern;
    private final KafkaConsumer<UUID, Event> consumer;

    private final Timer timer;

    private final Meter droppedEventsMeter;
    private final Meter filteredEventsMeter;
    private final Meter processedEventsMeter;
    private final Meter rejectedEventsMeter;
    private final Meter totalEventsMeter;

    public Sink(
            ExecutorService executor,
            String applicationId,
            Properties properties,
            Processor processor,
            Subscription subscription,
            EventDeserializer deserializer,
            MetricsCollector metricsCollector) {
        this(executor, applicationId, properties, processor, subscription, deserializer, metricsCollector, TimeSource.SYSTEM);
    }

    Sink(
            ExecutorService executor,
            String applicationId,
            Properties properties,
            Processor processor,
            Subscription subscription,
            EventDeserializer deserializer,
            MetricsCollector metricsCollector,
            TimeSource time) {
        this.executor = executor;
        this.processor = processor;

        this.filters = EventFilter.from(PropertiesUtil.ofScope(properties, "filter"));

        this.pollTimeoutMs = PropertiesUtil.get(Props.POLL_TIMEOUT_MS, properties).get();
        this.batchSize = PropertiesUtil.get(Props.BATCH_SIZE, properties).get();
        this.availabilityTimeoutMs = PropertiesUtil.get(Props.AVAILABILITY_TIMEOUT_MS, properties).get();

        String consumerGroupId =
                PropertiesUtil.get(Props.GROUP_ID, properties).
                        orEmpty(subscription.toGroupId(applicationId));

        this.pattern = subscription.toPattern();

        Properties consumerProperties = PropertiesUtil.ofScope(properties, Scopes.CONSUMER);
        consumerProperties.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroupId);
        consumerProperties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        consumerProperties.putIfAbsent(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, batchSize);
        consumerProperties.put(KafkaConfigs.METRICS_COLLECTOR_INSTANCE_CONFIG, metricsCollector);

        UuidDeserializer keyDeserializer = new UuidDeserializer();

        this.consumer = new KafkaConsumer<>(consumerProperties, keyDeserializer, deserializer);

        this.timer = time.timer(pollTimeoutMs);

        droppedEventsMeter = metricsCollector.meter("droppedEvents");
        filteredEventsMeter = metricsCollector.meter("filteredEvents");
        processedEventsMeter = metricsCollector.meter("processedEvents");
        rejectedEventsMeter = metricsCollector.meter("rejectedEvents");
        totalEventsMeter = metricsCollector.meter("totalEvents");
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
        while (isRunning()) {
            if (processor.isAvailable()) {
                try {

                    subscribe();

                    while (processor.isAvailable()) {
                        List<Event> events = new ArrayList<>(batchSize * 2);

                        int droppedEvents = 0;
                        int filteredEvents = 0;

                        timer.reset();

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

                            for (TopicPartition partition : partitions) {
                                List<ConsumerRecord<UUID, Event>> records = pollResult.records(partition);
                                for (ConsumerRecord<UUID, Event> record : records) {
                                    Event event = record.value();
                                    if (event == null) {// Received non-deserializable data, should be ignored
                                        droppedEvents++;
                                        continue;
                                    }
                                    if (!filter(event)) {
                                        filteredEvents++;
                                        continue;
                                    }
                                    events.add(event);
                                }
                            }
                        } while (events.size() < batchSize && !timer.isExpired());

                        ProcessorResult result = processor.process(events);
                        if (result.isSuccess()) {
                            try {
                                commit();
                                droppedEventsMeter.mark(droppedEvents);
                                filteredEventsMeter.mark(filteredEvents);
                                processedEventsMeter.mark(result.getProcessedEvents());
                                rejectedEventsMeter.mark(result.getRejectedEvents());
                                totalEventsMeter.mark(events.size());
                            } catch (CommitFailedException ex) {
                                LOGGER.warn("Commit failed due to rebalancing", ex);
                                continue;
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
    protected final ConsumerRecords<UUID, Event> poll(Duration timeout) throws WakeupException {
        return consumer.poll(timeout);
    }

    protected final void commit() {
        consumer.commitSync();
    }

    protected final void commit(Map<TopicPartition, OffsetAndMetadata> offsets) {
        consumer.commitSync(offsets);
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
