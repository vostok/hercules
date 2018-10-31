package ru.kontur.vostok.hercules.kafka.util.processing.single;

import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.Consumed;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.kafka.util.processing.SinkStatusFsm;
import ru.kontur.vostok.hercules.kafka.util.serialization.EventDeserializer;
import ru.kontur.vostok.hercules.kafka.util.serialization.EventSerde;
import ru.kontur.vostok.hercules.kafka.util.serialization.EventSerializer;
import ru.kontur.vostok.hercules.kafka.util.serialization.UuidSerde;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.util.PatternMatcher;
import ru.kontur.vostok.hercules.util.functional.Result;
import ru.kontur.vostok.hercules.util.properties.PropertyDescription;
import ru.kontur.vostok.hercules.util.properties.PropertyDescriptions;

import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * CommonSingleSink
 *
 * @author Kirill Sulim
 */
public class CommonSingleSink {

    private static class StreamProps {

        static final PropertyDescription<PatternMatcher> PATTERN = PropertyDescriptions
                .propertyOfType(PatternMatcher.class, "stream.pattern")
                .withParser(s -> {
                    try {
                        return Result.ok(new PatternMatcher(s));
                    }
                    catch (IllegalArgumentException e) {
                        return Result.error(e.getMessage());
                    }
                })
                .build();

    }

    private static final String GROUP_ID_PATTERN = "hercules.%s.%s";

    private final KafkaStreams kafkaStreams;
    private final SinkStatusFsm status = new SinkStatusFsm();


    public CommonSingleSink(
            String daemonId,
            Properties streamProperties,
            Properties sinkProperties,
            Supplier<SingleSender<UUID, Event>> senderSupplier,
            MetricsCollector metricsCollector
    ) {
        final PatternMatcher patternMatcher = StreamProps.PATTERN.extract(streamProperties);

        streamProperties.put(StreamsConfig.APPLICATION_ID_CONFIG, String.format(GROUP_ID_PATTERN, daemonId, patternMatcher.toString()));

        Serde<UUID> keySerde = new UuidSerde();
        EventSerializer serializer = new EventSerializer();
        EventDeserializer deserializer = EventDeserializer.parseAllTags();
        Serde<Event> valueSerde = new EventSerde(serializer, deserializer);

        final Meter receivedEventsMeter = metricsCollector.meter("receivedEvents");
        final Meter receivedEventsSizeMeter = metricsCollector.meter("receivedEventsSize");
        final Meter processedEventsMeter = metricsCollector.meter("processedEventsMeter");
        final Meter droppedEventsMeter = metricsCollector.meter("droppedEvents");
        final Timer processTimeTimer = metricsCollector.timer("processTime");

        StreamsBuilder builder = new StreamsBuilder();
        builder.stream(patternMatcher.getRegexp(), Consumed.with(keySerde, valueSerde)).process(() ->
            new CommonSingleSinkProcessor(
                    status,
                    senderSupplier.get(),
                    receivedEventsMeter,
                    receivedEventsSizeMeter,
                    processedEventsMeter,
                    droppedEventsMeter,
                    processTimeTimer
            )
        );

        kafkaStreams = new KafkaStreams(builder.build(), streamProperties);
    }

    public void start() {
        kafkaStreams.start();
        status.markInitCompleted();
    }

    public void stop() {
        kafkaStreams.close(5_000, TimeUnit.MILLISECONDS);
        status.stop();
    }
}
