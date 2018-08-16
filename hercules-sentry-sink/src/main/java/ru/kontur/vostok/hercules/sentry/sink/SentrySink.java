package ru.kontur.vostok.hercules.sentry.sink;

import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.streams.Consumed;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import ru.kontur.vostok.hercules.util.PatternMatcher;
import ru.kontur.vostok.hercules.kafka.util.serialization.EventDeserializer;
import ru.kontur.vostok.hercules.kafka.util.serialization.EventSerde;
import ru.kontur.vostok.hercules.kafka.util.serialization.EventSerializer;
import ru.kontur.vostok.hercules.kafka.util.serialization.UuidSerde;
import ru.kontur.vostok.hercules.protocol.Event;

import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author Gregory Koshelev
 */
public class SentrySink {

    private static final String ID_PREFIX = "hercules.sink.sentry.";

    private final KafkaStreams kafkaStreams;

    public SentrySink(Properties properties, PatternMatcher patternMatcher, SentrySyncProcessor syncProcessor) {
        properties.put(StreamsConfig.APPLICATION_ID_CONFIG, ID_PREFIX + patternMatcher.toString());

        Serde<UUID> keySerde = new UuidSerde();

        EventSerializer serializer = new EventSerializer();
        EventDeserializer deserializer = EventDeserializer.parseAllTags();
        Serde<Event> valueSerde = new EventSerde(serializer, deserializer);

        StreamsBuilder builder = new StreamsBuilder();
        builder.stream(patternMatcher.getRegexp(), Consumed.with(keySerde, valueSerde)).process(() -> syncProcessor);

        kafkaStreams = new KafkaStreams(builder.build(), properties);
    }

    public void start() {
        kafkaStreams.start();
    }

    public void stop(long timeout, TimeUnit timeUnit) {
        kafkaStreams.close(timeout, timeUnit);
    }
}
