package ru.kontur.vostok.hercules.kafka.util.processing;

import com.codahale.metrics.Meter;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.Serde;
import ru.kontur.vostok.hercules.kafka.util.serialization.EventDeserializer;
import ru.kontur.vostok.hercules.kafka.util.serialization.EventSerde;
import ru.kontur.vostok.hercules.kafka.util.serialization.EventSerializer;
import ru.kontur.vostok.hercules.kafka.util.serialization.UuidSerde;
import ru.kontur.vostok.hercules.metrics.MetricsCollector;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.util.PatternMatcher;
import ru.kontur.vostok.hercules.util.properties.PropertiesExtractor;
import ru.kontur.vostok.hercules.util.time.Timer;

import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;


/**
 * CommonBulkEventSink - common class for bulk processing of kafka streams content
 */
public class CommonBulkEventSink {

    private static final String POLL_TIMEOUT = "poll.timeout";
    private static final String BATCH_SIZE = "batch.size";

    private static final String ID_TEMPLATE = "hercules.sink.%s.%s";

    private final KafkaConsumer<UUID, Event> consumer;
    private final BulkSender<Event> eventSender;
    private final PatternMatcher streamPattern;
    private final int pollTimeout;
    private final int batchSize;

    private final Meter receivedEventsMeter;
    private final Meter processedEventsMeter;
    private final Meter droppedEventsMeter;
    private final com.codahale.metrics.Timer processTimeTimer;

    private volatile boolean running = true;

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
        streamsProperties.put("max.poll.interval.ms", pollTimeout * 10); // TODO: Find out how normal is this

        Serde<UUID> keySerde = new UuidSerde();
        Serde<Event> valueSerde = new EventSerde(new EventSerializer(), EventDeserializer.parseAllTags());

        this.consumer = new KafkaConsumer<>(streamsProperties, keySerde.deserializer(), valueSerde.deserializer());
        this.eventSender = eventSender;
        this.streamPattern = streamPattern;

        this.receivedEventsMeter = metricsCollector.meter("receivedEvents");
        this.processedEventsMeter = metricsCollector.meter("processedEvents");
        this.droppedEventsMeter = metricsCollector.meter("droppedEvents");
        this.processTimeTimer = metricsCollector.timer("processTime");
    }

    /**
     * Start sink
     */
    public void start() {
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
        while (running) {
            timer.reset().start();
            long timeLeft = pollTimeout;

            while (running && current.available() &&  0 <= timeLeft) {
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

        consumer.unsubscribe();
    }

    /**
     * Stop sink
     * @param timeout
     * @param timeUnit
     */
    public void stop(int timeout, TimeUnit timeUnit) {
        running = false;
        consumer.wakeup();
    }
}
