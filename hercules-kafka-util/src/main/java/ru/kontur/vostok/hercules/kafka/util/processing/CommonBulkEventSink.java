package ru.kontur.vostok.hercules.kafka.util.processing;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.Serde;
import ru.kontur.vostok.hercules.kafka.util.serialization.EventDeserializer;
import ru.kontur.vostok.hercules.kafka.util.serialization.EventSerde;
import ru.kontur.vostok.hercules.kafka.util.serialization.EventSerializer;
import ru.kontur.vostok.hercules.kafka.util.serialization.UuidSerde;
import ru.kontur.vostok.hercules.meta.stream.Stream;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;

import java.util.Collections;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class CommonBulkEventSink {

    private static final String POLL_TIMEOUT = "poll.timeout";
    private static final String BATCH_SIZE = "batch.size";

    private static final String ID_TEMPLATE = "hercules.sink.%s.%s";

    private final KafkaConsumer<UUID, Event> consumer;
    private final BulkSender<Event> eventSender;
    private final String streamName;
    private final int pollTimeout;
    private final int batchSize;

    private volatile boolean running = true;

    public CommonBulkEventSink(
            String destinationName,
            Stream stream,
            Properties streamsProperties,
            BulkSender<Event> eventSender
    ) {
        this.batchSize = PropertiesUtil.getAs(streamsProperties, BATCH_SIZE, Integer.class)
                .orElseThrow(PropertiesUtil.missingPropertyError(BATCH_SIZE));

        this.pollTimeout = PropertiesUtil.getAs(streamsProperties, POLL_TIMEOUT, Integer.class)
                .orElseThrow(PropertiesUtil.missingPropertyError(POLL_TIMEOUT));

        streamsProperties.put("group.id", String.format(ID_TEMPLATE, destinationName, stream.getName()));
        streamsProperties.put("enable.auto.commit", false);
        streamsProperties.put("max.poll.records", batchSize);
        streamsProperties.put("max.poll.interval.ms", pollTimeout * 10); // TODO: Find out how normal is this

        Serde<UUID> keySerde = new UuidSerde();
        Serde<Event> valueSerde = new EventSerde(new EventSerializer(), EventDeserializer.parseAllTags());

        this.consumer = new KafkaConsumer<>(streamsProperties, keySerde.deserializer(), valueSerde.deserializer());
        this.eventSender = eventSender;
        this.streamName = stream.getName();
    }

    public void start() {
        consumer.subscribe(Collections.singleton(streamName));

        RecordStorage<UUID, Event> current = new RecordStorage<>(batchSize);
        RecordStorage<UUID, Event> next = new RecordStorage<>(batchSize);


        /*
         * Try to poll new records from kafka until reached batchSize or timeout expired then process all
         * collected data. If the total count of polled records exceeded batchSize after the last poll extra records
         * will be saved in next record storage to process these records at the next step of iteration.
         */
        while (running) {
            int timeLeft = pollTimeout;
            while (running && current.available() && 0 < timeLeft) {
                long startTime = System.currentTimeMillis();
                ConsumerRecords<UUID, Event> poll = consumer.poll(timeLeft);
                for (ConsumerRecord<UUID, Event> record : poll) {
                    if (current.available()) {
                        current.add(record);
                    } else {
                        next.add(record);
                    }
                }

                int pollDuration = (int)(System.currentTimeMillis() - startTime);
                timeLeft -= pollDuration;
            }

            eventSender.accept(current.getRecords());
            consumer.commitSync(current.getOffsets(null));

            current = next;
            next = new RecordStorage<>(batchSize);
        }

        consumer.unsubscribe();
    }

    public void stop(int timeout, TimeUnit timeUnit) {
        running = false;
        consumer.wakeup();
    }
}
