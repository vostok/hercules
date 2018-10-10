package ru.kontur.vostok.hercules.kafka.util.processing;

import com.codahale.metrics.Meter;
import org.apache.kafka.clients.consumer.CommitFailedException;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.Serde;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.kafka.util.serialization.EventDeserializer;
import ru.kontur.vostok.hercules.kafka.util.serialization.EventSerde;
import ru.kontur.vostok.hercules.kafka.util.serialization.EventSerializer;
import ru.kontur.vostok.hercules.kafka.util.serialization.UuidSerde;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.util.PatternMatcher;
import ru.kontur.vostok.hercules.util.properties.PropertiesExtractor;
import ru.kontur.vostok.hercules.util.time.Timer;

import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * CommonBulkEventSink - common class for bulk processing of kafka streams content
 */
public class CommonBulkEventSink {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommonBulkEventSink.class);

    private static final String POLL_TIMEOUT = "poll.timeout";
    private static final String BATCH_SIZE = "batch.size";
    private static final String PING_RATE = "ping.rate";

    private static final int PING_RATE_DEFAULT_VALUE = 1000;

    private static final String ID_TEMPLATE = "hercules.sink.%s.%s";

    private final KafkaConsumer<UUID, Event> consumer;
    private final BulkSender<Event> eventSender;

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    private final PatternMatcher streamPattern;
    private final int pollTimeout;
    private final int batchSize;
    private final int pingRate;

    private final Meter receivedEventsMeter;
    private final Meter processedEventsMeter;
    private final Meter droppedEventsMeter;
    private final com.codahale.metrics.Timer processTimeTimer;

    private final CommonBulkSinkStatusFsm status = new CommonBulkSinkStatusFsm();

    /**
     * @param destinationName data flow destination name, where data must be copied
     * @param streamPattern stream which matches pattern will be processed by this sink
     * @param streamsProperties kafka streams properties
     * @param eventSender instance of bulk messages processor
     */
    public CommonBulkEventSink(
            String destinationName,
            PatternMatcher streamPattern,
            Properties streamsProperties,
            BulkSender<Event> eventSender,
            MetricsCollector metricsCollector
    ) {
        // TODO: Should be loaded from separate namespace. (see HERCULES-31)
        this.batchSize = PropertiesExtractor.getRequiredProperty(streamsProperties, BATCH_SIZE, Integer.class);
        this.pollTimeout = PropertiesExtractor.getRequiredProperty(streamsProperties, POLL_TIMEOUT, Integer.class);
        this.pingRate = PropertiesExtractor.getAs(streamsProperties, PING_RATE, Integer.class).orElse(PING_RATE_DEFAULT_VALUE);

        if (pollTimeout < 0) {
            throw new IllegalArgumentException("Poll timeout must be greater than 0");
        }

        final String groupId = String.format(ID_TEMPLATE, destinationName, streamPattern.toString())
                .replaceAll("\\s+", "-");

        streamsProperties.put("group.id", groupId);
        streamsProperties.put("enable.auto.commit", false);
        streamsProperties.put("max.poll.records", batchSize);

        Serde<UUID> keySerde = new UuidSerde();
        Serde<Event> valueSerde = new EventSerde(new EventSerializer(), EventDeserializer.parseAllTags());

        this.consumer = new KafkaConsumer<>(streamsProperties, keySerde.deserializer(), valueSerde.deserializer());
        this.eventSender = eventSender;
        this.streamPattern = streamPattern;

        this.receivedEventsMeter = metricsCollector.meter("receivedEvents");
        this.processedEventsMeter = metricsCollector.meter("processedEvents");
        this.droppedEventsMeter = metricsCollector.meter("droppedEvents");
        this.processTimeTimer = metricsCollector.timer("processTime");
        metricsCollector.status("status", status::getState);

        this.executor.scheduleAtFixedRate(this::ping, 0, pingRate, TimeUnit.MILLISECONDS);
    }

    /**
     * Start sink
     */
    public void run() {
        status.markInitCompleted();
        while (status.isRunning()) {
            try {
                status.waitForState(
                        CommonBulkSinkStatus.RUNNING,
                        CommonBulkSinkStatus.STOPPING_FROM_INIT,
                        CommonBulkSinkStatus.STOPPING_FROM_RUNNING,
                        CommonBulkSinkStatus.STOPPING_FROM_SUSPEND
                );
                if (!status.isRunning()) {
                    return;
                }

                consumer.subscribe(streamPattern.getRegexp());

                RecordStorage<UUID, Event> current = new RecordStorage<>(batchSize);
                RecordStorage<UUID, Event> next = new RecordStorage<>(batchSize);

                /*
                 * Try to poll new records from kafka until reached batchSize or timeout expired then process all
                 * collected data. If the total count of polled records exceeded batchSize after the last poll extra records
                 * will be saved in next record storage to process these records at the next step of iteration.
                 */
                TimeUnit unit = TimeUnit.MICROSECONDS;
                Timer timer = new Timer(unit, pollTimeout);
                while (status.isRunning()) {
                    timer.reset().start();
                    long timeLeft = pollTimeout;

                    while (status.isRunning() && current.available() && 0 <= timeLeft) {
                        try {
                            ConsumerRecords<UUID, Event> poll = consumer.poll(timeLeft);
                            for (ConsumerRecord<UUID, Event> record : poll) {
                                if (current.available()) {
                                    current.add(record);
                                } else {
                                    next.add(record);
                                }
                            }
                            timeLeft = timer.timeLeft();
                        }
                        catch (WakeupException e) {
                            /*
                             * Skip wakeup exception as it is termination signal,
                             * then try to process already polled data
                             */
                        }
                    }

                    int recordsSize = current.getRecords().size();

                    BulkSenderStat stat = eventSender.process(current.getRecords());
                    consumer.commitSync(current.getOffsets(null));

                    receivedEventsMeter.mark(recordsSize);
                    processedEventsMeter.mark(stat.getProcessed());
                    droppedEventsMeter.mark(stat.getDropped());
                    processTimeTimer.update(timer.elapsed(), unit);

                    current = next;
                    next = new RecordStorage<>(batchSize);
                }
            }
            catch (CommitFailedException e) {
                LOGGER.warn("Consumer was kicked by timeout");
            }
            catch (BackendServiceFailedException e) {
                LOGGER.error("Backend failed with", e);
                status.markBackendFailed();
            }
            catch (InterruptedException e) {
                LOGGER.error("Waiting was interrupted", e);
            }
            catch (ExecutionException e) {
                LOGGER.error("Execution exception", e);
            }
            finally {
                consumer.unsubscribe();
            }
        }
    }

    /**
     * Stop sink
     * @param timeout
     * @param timeUnit
     */
    public void stop(int timeout, TimeUnit timeUnit) {
        status.stop();
        consumer.wakeup();
    }

    private void ping() {
        try {
            if (eventSender.ping()) {
                status.markBackendAlive();
            }
            else {
                status.markBackendFailed();
            }
        }
        catch (Throwable e) {
            LOGGER.error("Ping error should never happen, stopping service", e);
            System.exit(1);
            throw e;
        }
    }
}
