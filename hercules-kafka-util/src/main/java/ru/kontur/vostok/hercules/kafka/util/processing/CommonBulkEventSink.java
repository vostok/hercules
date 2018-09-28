package ru.kontur.vostok.hercules.kafka.util.processing;

import com.codahale.metrics.Meter;
import org.apache.kafka.clients.consumer.CommitFailedException;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.protocol.types.Field;
import org.apache.kafka.common.serialization.Serde;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.health.ServiceStatus;
import ru.kontur.vostok.hercules.kafka.util.serialization.EventDeserializer;
import ru.kontur.vostok.hercules.kafka.util.serialization.EventSerde;
import ru.kontur.vostok.hercules.kafka.util.serialization.EventSerializer;
import ru.kontur.vostok.hercules.kafka.util.serialization.UuidSerde;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.util.PatternMatcher;
import ru.kontur.vostok.hercules.util.properties.PropertiesExtractor;
import ru.kontur.vostok.hercules.util.time.Timer;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;


/**
 * CommonBulkEventSink - common class for bulk processing of kafka streams content
 */
public class CommonBulkEventSink {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommonBulkEventSink.class);

    private static final String POLL_TIMEOUT = "poll.timeout";
    private static final String BATCH_SIZE = "batch.size";

    private static final String ID_TEMPLATE = "hercules.sink.%s.%s";

    private final KafkaConsumer<UUID, Event> consumer;
    private final BulkSender<Event> eventSender;

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    private final PatternMatcher streamPattern;
    private final int pollTimeout;
    private final int batchSize;

    private final Meter receivedEventsMeter;
    private final Meter processedEventsMeter;
    private final Meter droppedEventsMeter;
    private final com.codahale.metrics.Timer processTimeTimer;

    private final AtomicReference<CommonBulkSinkStatus> status = new AtomicReference<>(CommonBulkSinkStatus.INIT);
    private volatile CountDownLatch statusChange;

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
        this.batchSize = PropertiesExtractor.getAs(streamsProperties, BATCH_SIZE, Integer.class)
                .orElseThrow(PropertiesExtractor.missingPropertyError(BATCH_SIZE));

        this.pollTimeout = PropertiesExtractor.getAs(streamsProperties, POLL_TIMEOUT, Integer.class)
                .orElseThrow(PropertiesExtractor.missingPropertyError(POLL_TIMEOUT));

        if (pollTimeout < 0) {
            throw new IllegalArgumentException("Poll timeout must be greater than 0");
        }

        streamsProperties.put("group.id", String.format(ID_TEMPLATE, destinationName, streamPattern.toString()));
        streamsProperties.put("enable.auto.commit", false);
        streamsProperties.put("max.poll.records", batchSize);
        streamsProperties.put("max.poll.interval.ms", 1000); // TODO: Find out how normal is this

        Serde<UUID> keySerde = new UuidSerde();
        Serde<Event> valueSerde = new EventSerde(new EventSerializer(), EventDeserializer.parseAllTags());

        this.consumer = new KafkaConsumer<>(streamsProperties, keySerde.deserializer(), valueSerde.deserializer());
        this.eventSender = eventSender;
        this.streamPattern = streamPattern;

        this.receivedEventsMeter = metricsCollector.meter("receivedEvents");
        this.processedEventsMeter = metricsCollector.meter("processedEvents");
        this.droppedEventsMeter = metricsCollector.meter("droppedEvents");
        this.processTimeTimer = metricsCollector.timer("processTime");
        metricsCollector.status("status", status::get);

        this.executor.scheduleAtFixedRate(this::ping, 0, 1000, TimeUnit.MILLISECONDS);
        this.statusChange = new CountDownLatch(1);
    }

    /**
     * Start sink
     */
    public void run() {
        if (!status.compareAndSet(CommonBulkSinkStatus.INIT, CommonBulkSinkStatus.RUNNING)) {
            throw new IllegalStateException("Some problems");
        }
        while (isRunning()) {
            try {
                waitForStatus(
                        CommonBulkSinkStatus.RUNNING,
                        CommonBulkSinkStatus.STOPPING_FROM_INIT,
                        CommonBulkSinkStatus.STOPPING_FROM_RUNNING,
                        CommonBulkSinkStatus.STOPPING_FROM_SUSPEND
                );
                if (!isRunning()) {
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
                while (isRunning()) {
                    timer.reset().start();
                    long timeLeft = pollTimeout;

                    while (isRunning() && current.available() && 0 <= timeLeft) {
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
                markBackendFailed();
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
        if (!(
                status.compareAndSet(CommonBulkSinkStatus.INIT, CommonBulkSinkStatus.STOPPING_FROM_INIT)
                || status.compareAndSet(CommonBulkSinkStatus.RUNNING, CommonBulkSinkStatus.STOPPING_FROM_RUNNING)
                || status.compareAndSet(CommonBulkSinkStatus.SUSPEND, CommonBulkSinkStatus.STOPPING_FROM_SUSPEND)
        )) {
            throw new IllegalStateException(String.format("Cannot start stopping in status %s", status.get()));
        }
        consumer.wakeup();
        statusChange.countDown();
    }

    private boolean isRunning() {
        switch (status.get()) {
            case INIT:
            case RUNNING:
            case SUSPEND:
                return true;
            case STOPPING_FROM_INIT:
            case STOPPING_FROM_RUNNING:
            case STOPPING_FROM_SUSPEND:
            case STOPPED:
                return false;
            default:
                throw new IllegalStateException("Unknown status");
        }
    }

    private void ping() {
        try {
            if (eventSender.ping()) {
                markBackendAlive();
            }
            else {
                markBackendFailed();
            }
        }
        catch (Exception e) {
            LOGGER.error("Ping error", e);
            throw new RuntimeException(e);
        }
    }

    private void markBackendAlive() {
        if (!(status.compareAndSet(CommonBulkSinkStatus.SUSPEND, CommonBulkSinkStatus.RUNNING)
                || status.compareAndSet(CommonBulkSinkStatus.RUNNING, CommonBulkSinkStatus.RUNNING)
                || status.compareAndSet(CommonBulkSinkStatus.INIT, CommonBulkSinkStatus.INIT)
                || status.compareAndSet(CommonBulkSinkStatus.STOPPING_FROM_INIT, CommonBulkSinkStatus.STOPPING_FROM_INIT)
                || status.compareAndSet(CommonBulkSinkStatus.STOPPING_FROM_RUNNING, CommonBulkSinkStatus.STOPPING_FROM_RUNNING)
                || status.compareAndSet(CommonBulkSinkStatus.STOPPING_FROM_SUSPEND, CommonBulkSinkStatus.STOPPING_FROM_SUSPEND)
        )) {
            throw new IllegalStateException(String.format("Cannot mark backend alive for status %s", status.get()));
        }
        statusChange.countDown();
    }

    private void markBackendFailed() {
        if (!(status.compareAndSet(CommonBulkSinkStatus.RUNNING, CommonBulkSinkStatus.SUSPEND)
                || status.compareAndSet(CommonBulkSinkStatus.SUSPEND, CommonBulkSinkStatus.SUSPEND)
                || status.compareAndSet(CommonBulkSinkStatus.INIT, CommonBulkSinkStatus.INIT)
                || status.compareAndSet(CommonBulkSinkStatus.STOPPING_FROM_INIT, CommonBulkSinkStatus.STOPPING_FROM_INIT)
                || status.compareAndSet(CommonBulkSinkStatus.STOPPING_FROM_RUNNING, CommonBulkSinkStatus.STOPPING_FROM_SUSPEND)
                || status.compareAndSet(CommonBulkSinkStatus.STOPPING_FROM_SUSPEND, CommonBulkSinkStatus.STOPPING_FROM_SUSPEND)
        )) {
            throw new IllegalStateException(String.format("Cannot mark backend alive for status %s", status.get()));
        }
        statusChange.countDown();
    }

    private void waitForStatus(CommonBulkSinkStatus ... statuses) {
        Set<CommonBulkSinkStatus> statusSet = new HashSet<>(Arrays.asList(statuses));
        while (!statusSet.contains(status.get())) {
            try {
                statusChange.await();
                statusChange = new CountDownLatch(1);
            }
            catch (InterruptedException e) {
                /* skip */
            }
        }
    }
}
