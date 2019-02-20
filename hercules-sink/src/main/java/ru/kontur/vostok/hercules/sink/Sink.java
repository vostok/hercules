package ru.kontur.vostok.hercules.sink;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.configuration.Scopes;
import ru.kontur.vostok.hercules.configuration.util.PropertiesUtil;
import ru.kontur.vostok.hercules.kafka.util.processing.bulk.ConsumerUtil;
import ru.kontur.vostok.hercules.kafka.util.serialization.EventDeserializer;
import ru.kontur.vostok.hercules.kafka.util.serialization.UuidDeserializer;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.util.PatternMatcher;
import ru.kontur.vostok.hercules.util.properties.PropertyDescription;
import ru.kontur.vostok.hercules.util.properties.PropertyDescriptions;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author Gregory Koshelev
 */
public abstract class Sink {
    private static final Logger LOGGER = LoggerFactory.getLogger(Sink.class);

    private volatile boolean running = false;

    private final ExecutorService executor;
    private final String application;

    protected final Properties properties;

    private final Duration pollTimeout;
    private final int batchSize;

    private final Pattern pattern;
    private final KafkaConsumer<UUID, Event> consumer;

    protected Sink(ExecutorService executor, String applicationId, Properties properties) {
        this.executor = executor;
        this.application = applicationId;
        this.properties = properties;

        this.pollTimeout = Duration.ofMillis(Props.POLL_TIMEOUT_MS.extract(properties));
        this.batchSize = Props.BATCH_SIZE.extract(properties);

        List<PatternMatcher> patternMatchers = Props.PATTERN.extract(properties).stream().
                map(PatternMatcher::new).collect(Collectors.toList());
        String consumerGroupId = ConsumerUtil.toGroupId(applicationId, patternMatchers);

        this.pattern = PatternMatcher.matcherListToRegexp(patternMatchers);

        Properties consumerProperties = PropertiesUtil.ofScope(properties, Scopes.CONSUMER);
        consumerProperties.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroupId);
        consumerProperties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        consumerProperties.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, batchSize);

        UuidDeserializer keyDeserializer = new UuidDeserializer();
        EventDeserializer valueDeserializer = EventDeserializer.parseAllTags();

        this.consumer = new KafkaConsumer<>(consumerProperties, keyDeserializer, valueDeserializer);

    }

    /**
     * Start sink.
     */
    public final void start() {
        running = true;

        executor.execute(this::run);
    }

    /**
     * Stop Sink.
     */
    public final void stop() {
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
     * Main Sink logic. Sink should control {@link #isRunning()} during run operation.
     */
    protected abstract void run();

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
    protected final ConsumerRecords<UUID, Event> poll() throws WakeupException {
        return consumer.poll(pollTimeout);
    }

    protected final void commit() {
        consumer.commitSync();
    }

    protected final void commit(Map<TopicPartition, OffsetAndMetadata> offsets) {
        consumer.commitSync(offsets);
    }

    private static class Props {
        static final PropertyDescription<Long> POLL_TIMEOUT_MS =
                PropertyDescriptions.longProperty("pollTimeoutMs").withDefaultValue(6_000L).build();

        static final PropertyDescription<Integer> BATCH_SIZE =
                PropertyDescriptions.integerProperty("batchSize").withDefaultValue(1000).build();

        static final PropertyDescription<List<String>> PATTERN = PropertyDescriptions
                .listOfStringsProperty("pattern")
                .build();
    }
}
