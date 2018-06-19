package ru.kontur.vostok.hercules.elasticsearch.sink;

import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.streams.Consumed;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import ru.kontur.vostok.hercules.kafka.util.processing.BulkProcessor;
import ru.kontur.vostok.hercules.kafka.util.serialization.EventDeserializer;
import ru.kontur.vostok.hercules.kafka.util.serialization.EventSerde;
import ru.kontur.vostok.hercules.kafka.util.serialization.EventSerializer;
import ru.kontur.vostok.hercules.kafka.util.serialization.VoidSerde;
import ru.kontur.vostok.hercules.meta.stream.Stream;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class ElasticSearchSink {

    private static final String ID_PREFIX = "hercules.elasticsearch-sink.stream.";

    public static final String PUNCTUATION_INTERVAL = "punctuation.interval";
    public static final int PUNCTUATION_INTERVAL_DEFAULT_VALUE = 1_000;

    public static final String BATCH_SIZE = "batch.size";
    public static final int BATCH_SIZE_DEFAULT_VALUE = 100_000;

    private final KafkaStreams kafkaStreams;

    public ElasticSearchSink(Stream stream, Properties streamsProperties, ElasticSearchEventSender eventSender) {
        streamsProperties.put(StreamsConfig.APPLICATION_ID_CONFIG, ID_PREFIX + stream.getName());

        int punctuationInterval = PropertiesUtil.get(streamsProperties, PUNCTUATION_INTERVAL, PUNCTUATION_INTERVAL_DEFAULT_VALUE);
        int batchZie = PropertiesUtil.get(streamsProperties, BATCH_SIZE, BATCH_SIZE_DEFAULT_VALUE);

        Serde<Void> keySerde = new VoidSerde();

        EventSerializer serializer = new EventSerializer();
        EventDeserializer deserializer = EventDeserializer.parseAllTags();
        Serde<Event> valueSerde = new EventSerde(serializer, deserializer);

        StreamsBuilder streamsBuilder = new StreamsBuilder();
        streamsBuilder.<Void, Event>stream(stream.getName(), Consumed.with(keySerde, valueSerde))
                .process(() -> new BulkProcessor<>(eventSender::send, batchZie, punctuationInterval));

        this.kafkaStreams = new KafkaStreams(streamsBuilder.build(), streamsProperties);
    }

    public void start() {
        kafkaStreams.start();
    }

    public void stop(int timeout, TimeUnit timeUnit) {
        kafkaStreams.close(timeout, timeUnit);
    }
}
