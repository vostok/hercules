package ru.kontur.vostok.hercules.kafka.util.processing;

import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.streams.Consumed;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import ru.kontur.vostok.hercules.kafka.util.serialization.EventDeserializer;
import ru.kontur.vostok.hercules.kafka.util.serialization.EventSerde;
import ru.kontur.vostok.hercules.kafka.util.serialization.EventSerializer;
import ru.kontur.vostok.hercules.kafka.util.serialization.UuidSerde;
import ru.kontur.vostok.hercules.meta.stream.Stream;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;

import java.util.Collection;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class CommonBulkEventSink {

    public static final String PUNCTUATION_INTERVAL = "punctuation.interval";
    public static final String BATCH_SIZE = "batch.size";

    private static final String ID_TEMPLATE = "hercules.sink.%s.%s";

    private final KafkaStreams kafkaStreams;

    public CommonBulkEventSink(
            String destinationName,
            Stream stream,
            Properties streamsProperties,
            Consumer<Collection<Entry<UUID, Event>>> bulkConsumer
    ) {
        streamsProperties.put(StreamsConfig.APPLICATION_ID_CONFIG, String.format(ID_TEMPLATE, destinationName, stream.getName()));
        streamsProperties.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, -1); // Disable auto commit

        int punctuationInterval = PropertiesUtil.getAs(streamsProperties, PUNCTUATION_INTERVAL, Integer.class)
                .orElseThrow(PropertiesUtil.missingPropertyError(PUNCTUATION_INTERVAL));

        int batchZie = PropertiesUtil.getAs(streamsProperties, BATCH_SIZE, Integer.class)
                .orElseThrow(PropertiesUtil.missingPropertyError(BATCH_SIZE));


        Serde<UUID> keySerde = new UuidSerde();

        EventSerializer serializer = new EventSerializer();
        EventDeserializer deserializer = EventDeserializer.parseAllTags();
        Serde<Event> valueSerde = new EventSerde(serializer, deserializer);

        StreamsBuilder streamsBuilder = new StreamsBuilder();
        streamsBuilder
                .stream(stream.getName(), Consumed.with(keySerde, valueSerde))
                .process(() -> new BulkProcessor<>(bulkConsumer, batchZie, punctuationInterval));

        this.kafkaStreams = new KafkaStreams(streamsBuilder.build(), streamsProperties);
    }

    public void start() {
        kafkaStreams.start();
    }

    public void stop(int timeout, TimeUnit timeUnit) {
        kafkaStreams.close(timeout, timeUnit);
    }
}
