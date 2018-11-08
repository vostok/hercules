package ru.kontur.vostok.hercules.kafka.util.processing.single;

import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.Consumed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.kafka.util.processing.ServicePinger;
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
import ru.kontur.vostok.hercules.util.validation.Validators;

import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
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
                    } catch (IllegalArgumentException e) {
                        return Result.error(e.getMessage());
                    }
                })
                .build();
    }

    private static class SinkProps {

        static final PropertyDescription<Integer> PING_RATE_MS = PropertyDescriptions
                .integerProperty("ping.rate")
                .withDefaultValue(1_000)
                .withValidator(Validators.greaterOrEquals(0))
                .build();
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(CommonSingleSink.class);

    private static final String GROUP_ID_PATTERN = "hercules.%s.%s";

    private final KafkaStreams kafkaStreams;
    private final SinkStatusFsm status = new SinkStatusFsm();

    private final ServicePinger pinger;
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private final int pingRate;


    public CommonSingleSink(
            String daemonId,
            Properties streamProperties,
            Properties sinkProperties,
            Supplier<SingleSender<UUID, Event>> senderSupplier,
            Supplier<ServicePinger> pingerSupplier,
            MetricsCollector metricsCollector
    ) {
        final PatternMatcher patternMatcher = StreamProps.PATTERN.extract(streamProperties);

        streamProperties.put(StreamsConfig.APPLICATION_ID_CONFIG, String.format(GROUP_ID_PATTERN, daemonId, patternMatcher.toString()));

        Serde<UUID> keySerde = new UuidSerde();
        EventSerializer serializer = new EventSerializer();
        EventDeserializer deserializer = EventDeserializer.parseAllTags();
        Serde<Event> valueSerde = new EventSerde(serializer, deserializer);

        final Meter receivedEventsMeter = metricsCollector.meter("receivedEvents");
        final Meter receivedEventsSizeMeter = metricsCollector.meter("receivedEventsSizeBytes");
        final Meter processedEventsMeter = metricsCollector.meter("processedEvents");
        final Meter droppedEventsMeter = metricsCollector.meter("droppedEvents");
        final Timer processTimeTimer = metricsCollector.timer("processTimeMs");
        metricsCollector.status("status", status::getState);

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

        this.kafkaStreams = new KafkaStreams(builder.build(), streamProperties);

        this.pingRate = SinkProps.PING_RATE_MS.extract(sinkProperties);
        this.pinger = pingerSupplier.get();
    }

    public void start() {
        executor.scheduleAtFixedRate(this::ping, 0, pingRate, TimeUnit.MILLISECONDS);
        kafkaStreams.start();
        status.markInitCompleted();
    }

    public void stop() {
        status.stop();
        kafkaStreams.close(5_000, TimeUnit.MILLISECONDS);
        executor.shutdown();
    }

    private void ping() {
        try {
            if (pinger.ping()) {
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
