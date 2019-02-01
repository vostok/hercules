package ru.kontur.vostok.hercules.kafka.util.processing.bulk;

import com.codahale.metrics.Meter;
import org.apache.kafka.clients.consumer.CommitFailedException;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.Serde;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.kafka.util.processing.BackendServiceFailedException;
import ru.kontur.vostok.hercules.kafka.util.processing.SinkStatus;
import ru.kontur.vostok.hercules.kafka.util.processing.SinkStatusFsm;
import ru.kontur.vostok.hercules.kafka.util.serialization.EventDeserializer;
import ru.kontur.vostok.hercules.kafka.util.serialization.EventSerde;
import ru.kontur.vostok.hercules.kafka.util.serialization.EventSerializer;
import ru.kontur.vostok.hercules.kafka.util.serialization.UuidSerde;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.util.PatternMatcher;
import ru.kontur.vostok.hercules.util.logging.LoggingConstants;
import ru.kontur.vostok.hercules.util.properties.PropertyDescription;
import ru.kontur.vostok.hercules.util.properties.PropertyDescriptions;
import ru.kontur.vostok.hercules.util.time.Timer;
import ru.kontur.vostok.hercules.util.validation.Validators;

import java.util.Objects;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * BulkConsumer
 *
 * @author Kirill Sulim
 */
public class BulkConsumer implements Runnable {

    private static class Props {
        static final PropertyDescription<Integer> BATCH_SIZE = PropertyDescriptions
                .integerProperty("batch.size")
                .build();

        static final PropertyDescription<Integer> POLL_TIMEOUT_MS = PropertyDescriptions
                .integerProperty("poll.timeout")
                .build();

        static final PropertyDescription<Integer> QUEUE_SIZE = PropertyDescriptions
                .integerProperty("queue.size")
                .withDefaultValue(64)
                .withValidator(Validators.greaterThan(0))
                .build();
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(BulkConsumer.class);
    private static final Logger DROPPED_EVENTS_LOGGER = LoggerFactory.getLogger(LoggingConstants.DROPPED_EVENT_LOGGER_NAME);

    private final KafkaConsumer<UUID, Event> consumer;
    private final PatternMatcher streamPattern;
    private final int pollTimeout;
    private final int batchSize;

    private final SinkStatusFsm status;
    private final BulkQueue<UUID, Event> queue;
    private final BulkSenderPool<UUID, Event> senderPool;

    private final Meter receivedEventsMeter;
    private final Meter receivedEventsSizeMeter;
    private final Meter processedEventsMeter;
    private final Meter droppedEventsMeter;
    private final com.codahale.metrics.Timer processTimeTimer;


    public BulkConsumer(
            String id,
            Properties streamsProperties,
            Properties sinkProperties,
            PatternMatcher streamPattern,
            String consumerGroupId,
            SinkStatusFsm status,
            Supplier<BulkSender<Event>> senderFactory,
            Meter receivedEventsMeter,
            Meter receivedEventsSizeMeter,
            Meter processedEventsMeter,
            Meter droppedEventsMeter,
            com.codahale.metrics.Timer processTimeTimer
    ) {
        this.batchSize = Props.BATCH_SIZE.extract(sinkProperties);
        this.pollTimeout = Props.POLL_TIMEOUT_MS.extract(sinkProperties);

        streamsProperties.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroupId);
        streamsProperties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        streamsProperties.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, batchSize);

        Serde<UUID> keySerde = new UuidSerde();
        Serde<Event> valueSerde = new EventSerde(new EventSerializer(), EventDeserializer.parseAllTags());

        this.streamPattern = streamPattern;
        this.consumer = new KafkaConsumer<>(
            streamsProperties,
            keySerde.deserializer(),
            valueSerde.deserializer()
        );

        this.status = status;

        final int queueSize = Props.QUEUE_SIZE.extract(sinkProperties);
        this.queue = new BulkQueue<>(queueSize, status);

        this.senderPool = new BulkSenderPool<>(
                id,
                sinkProperties,
                queue,
                senderFactory,
                status
        );

        this.receivedEventsMeter = receivedEventsMeter;
        this.receivedEventsSizeMeter = receivedEventsSizeMeter;
        this.processedEventsMeter = processedEventsMeter;
        this.droppedEventsMeter = droppedEventsMeter;
        this.processTimeTimer = processTimeTimer;
    }

    @Override
    public void run() {
        senderPool.start();

        while (status.isRunning()) {
            try {
                try {
                    status.waitForState(
                            SinkStatus.RUNNING,
                            SinkStatus.STOPPING_FROM_INIT,
                            SinkStatus.STOPPING_FROM_RUNNING,
                            SinkStatus.STOPPING_FROM_SUSPEND
                    );
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException("Should never happened", e);
                }
                if (!status.isRunning()) {
                    return;
                }

                consumer.subscribe(streamPattern.getRegexp());

                RecordStorage<UUID, Event> current = new RecordStorage<>(batchSize);
                RecordStorage<UUID, Event> next = new RecordStorage<>(batchSize);

                TimeUnit unit = TimeUnit.MICROSECONDS;
                Timer timer = new Timer(unit, pollTimeout);
                while (status.isRunning()) {
                    /*
                     * Polling phase
                     *
                     * Try to poll new records from kafka until reached batchSize or timeout expired then execute all
                     * collected data. If the total count of polled records exceeded batchSize after the last poll extra records
                     * will be saved in next record storage to execute these records at the next step of iteration.
                     */
                    timer.reset().start();
                    long timeLeft = pollTimeout;

                    while (current.available() && 0 <= timeLeft) {
                        try {
                            // TODO: use poll(Duration)
                            ConsumerRecords<UUID, Event> poll = consumer.poll(timeLeft);
                            for (ConsumerRecord<UUID, Event> record : poll) {
                                if (Objects.nonNull(record.value())) {
                                    if (current.available()) {
                                        current.add(record);
                                    } else {
                                        next.add(record);
                                    }
                                } else {
                                    receivedEventsMeter.mark();

                                    droppedEventsMeter.mark();
                                    DROPPED_EVENTS_LOGGER.trace("{}", record.key());
                                }
                            }
                            timeLeft = timer.timeLeft();
                        } catch (WakeupException e) {
                            /*
                             * Skip wakeup exception as it is termination signal,
                             * then execute already polled data
                             */
                            break;
                        }
                    }
                    int count = current.getRecords().size();
                    receivedEventsMeter.mark(count);
                    receivedEventsSizeMeter.mark(
                            current.getRecords().stream().
                                    mapToInt(event -> event.getBytes().length).
                                    sum());

                    /*
                     * Queuing phase
                     *
                     * Put all polled data in sender pool queue and get future for processing result
                     */
                    if (0 < count) {
                        queue.put(current);
                    }

                    /*
                     * Commit phase
                     *
                     * Send statistics of processed data and commit last fully processed offset
                     */
                    BulkQueue.RunResult<UUID, Event> lastCommitOrNull = queue.getLastCommitOrNull();

                    if (Objects.nonNull(lastCommitOrNull)) {
                        RecordStorage<UUID, Event> storage = lastCommitOrNull.getStorage();
                        try {
                            consumer.commitSync(storage.getOffsets(null));
                        } catch (WakeupException e) {
                            /* Ignore cause this is termination signal */
                        }

                        BulkSenderStat stat = lastCommitOrNull.getStat();

                        processedEventsMeter.mark(stat.getProcessed());
                        droppedEventsMeter.mark(stat.getDropped());
                        processTimeTimer.update(timer.elapsed(), unit);
                    }

                    current = next;
                    next = new RecordStorage<>(batchSize);
                }
            } catch (CommitFailedException e) {
                LOGGER.warn("Consumer was kicked by timeout");
            } catch (BackendServiceFailedException e) {
                LOGGER.error("Backend failed with", e);
                status.markBackendFailed();
            } finally {
                consumer.unsubscribe();
                queue.purge();
            }
        }
        try {
            senderPool.stop();
        } catch (InterruptedException e) {
            throw new RuntimeException("Should never happened", e);
        }
    }

    public void wakeup() {
        consumer.wakeup();
    }
}
