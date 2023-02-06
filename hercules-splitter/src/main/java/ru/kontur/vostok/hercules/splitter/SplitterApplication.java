package ru.kontur.vostok.hercules.splitter;

import java.time.Duration;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KafkaStreams.State;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.jetbrains.annotations.NotNull;
import ru.kontur.vostok.hercules.application.Application;
import ru.kontur.vostok.hercules.application.Container;
import ru.kontur.vostok.hercules.configuration.Scopes;
import ru.kontur.vostok.hercules.health.CommonMetrics;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.http.HttpServer;
import ru.kontur.vostok.hercules.http.handler.InstrumentedRouteHandlerBuilder;
import ru.kontur.vostok.hercules.http.handler.KafkaStreamsStatusHttpHandler;
import ru.kontur.vostok.hercules.http.handler.RouteHandler;
import ru.kontur.vostok.hercules.kafka.util.KafkaConfigs;
import ru.kontur.vostok.hercules.kafka.util.consumer.Subscription;
import ru.kontur.vostok.hercules.kafka.util.metrics.GraphiteReporter;
import ru.kontur.vostok.hercules.kafka.util.serialization.UuidSerde;
import ru.kontur.vostok.hercules.protocol.TinyString;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.protocol.decoder.ContainerReader;
import ru.kontur.vostok.hercules.protocol.decoder.EventReader;
import ru.kontur.vostok.hercules.protocol.decoder.Reader;
import ru.kontur.vostok.hercules.protocol.decoder.VariantReader;
import ru.kontur.vostok.hercules.splitter.models.Envelope;
import ru.kontur.vostok.hercules.splitter.serialization.DestinationReader;
import ru.kontur.vostok.hercules.splitter.serialization.EnvelopDeserializer;
import ru.kontur.vostok.hercules.splitter.serialization.EnvelopeSerializer;
import ru.kontur.vostok.hercules.splitter.serialization.StreamingAlgorithmReader;
import ru.kontur.vostok.hercules.splitter.service.StreamingHasher;
import ru.kontur.vostok.hercules.splitter.service.StreamingHashersFactory;
import ru.kontur.vostok.hercules.splitter.service.spreader.RendezvousStreamingSpreader;
import ru.kontur.vostok.hercules.undertow.util.UndertowHttpServer;
import ru.kontur.vostok.hercules.util.lifecycle.Stoppable;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;

/**
 * Splitter application entry point class.
 * <p>
 * Application splits data from the source streams to the destination streams using hash functions.
 *
 * @author Aleksandr Yuferov
 */
public class SplitterApplication {

    private static final String NODE_NAME_PLACEHOLDER = "<node>";

    public static void main(String[] args) {
        var app = new SplitterApplication();
        Application.run("Hercules Splitter", "splitter", args, app::start);
    }

    private final AtomicReference<KafkaStreams.State> kafkaStreamsState = new AtomicReference<>(State.CREATED);

    /**
     * Start the application.
     *
     * @param properties Properties from given properties-file.
     * @param container  Container of objects that should be closed/stopped on application shutdown.
     */
    public void start(Properties properties, Container container) {
        MetricsCollector metricsCollector = createMetricsCollector(properties);
        container.register(metricsCollector);
        container.register(createKafkaStreams(properties, metricsCollector));
        container.register(createHttpServer(properties, metricsCollector));
    }

    /**
     * {@link MetricsCollector} factory.
     *
     * @param properties Application configuration properties.
     * @return Created bean.
     */
    @NotNull
    MetricsCollector createMetricsCollector(Properties properties) {
        MetricsCollector metricsCollector = new MetricsCollector(PropertiesUtil.ofScope(properties, Scopes.METRICS));
        CommonMetrics.registerCommonMetrics(metricsCollector);
        return metricsCollector;
    }


    /**
     * {@link KafkaStreams} factory.
     *
     * @param properties       Application configuration properties.
     * @param metricsCollector Application {@link MetricsCollector} instance.
     * @return Finish token.
     */
    @NotNull
    Stoppable createKafkaStreams(Properties properties, MetricsCollector metricsCollector) {
        Topology topology = createTopology(properties);

        Properties streamsProperties = PropertiesUtil.ofScope(properties, Scopes.KAFKA_STREAMS);
        streamsProperties.setProperty(StreamsConfig.METRIC_REPORTER_CLASSES_CONFIG, GraphiteReporter.class.getCanonicalName());
        streamsProperties.put(KafkaConfigs.METRICS_COLLECTOR_INSTANCE_CONFIG, metricsCollector);

        var streams = new KafkaStreams(topology, streamsProperties);
        streams.setStateListener((newState, oldState) -> kafkaStreamsState.set(newState));
        streams.cleanUp();
        streams.start();
        return (amount, unit) -> streams.close(Duration.ofMillis(unit.toMillis(amount)));
    }

    /**
     * Kafka Streams {@link Topology} factory.
     *
     * @param properties Application configuration properties.
     * @return Created bean.
     */
    @NotNull
    Topology createTopology(Properties properties) {
        Subscription subscription = Subscription.builder()
                .include(PropertiesUtil.get(ConfigurationProperties.PATTERN, properties).get())
                .exclude(PropertiesUtil.get(ConfigurationProperties.PATTERN_EXCLUSIONS, properties).get())
                .build();
        Serde<UUID> idSerde = new UuidSerde();
        Serde<Envelope> envelopeSerde = Serdes.serdeFrom(new EnvelopeSerializer(), createEnvelopeDeserializer(properties));
        Consumed<UUID, Envelope> consumed = Consumed.with(idSerde, envelopeSerde);
        String destination = PropertiesUtil.get(ConfigurationProperties.OUTPUT_STREAM_TEMPLATE, properties).get();

        var streamsBuilder = new StreamsBuilder();
        streamsBuilder.stream(subscription.toPattern(), consumed)
                .filter((id, envelope) -> envelope != null)
                .to((id, envelope, context) -> destination.replace(NODE_NAME_PLACEHOLDER, envelope.destination()));

        return streamsBuilder.build();
    }

    /**
     * {@link Deserializer} factory of {@link Envelope} objects for Kafka Client library.
     * <p>
     * Creates deserializer that under the hood uses streaming hash algorithm.
     *
     * @param properties Application configuration properties.
     * @return Created bean.
     */
    @NotNull
    Deserializer<Envelope> createEnvelopeDeserializer(Properties properties) {
        Set<TinyString> shardingKeys = Arrays.stream(PropertiesUtil.get(ConfigurationProperties.SHARDING_KEYS, properties).get())
                .map(TinyString::of)
                .collect(Collectors.toUnmodifiableSet());
        String hashAlgorithm = PropertiesUtil.get(ConfigurationProperties.HASH_ALGORITHM, properties).get();
        Map<String, Double> partitionWeights = PropertiesUtil.ofScope(properties, ConfigurationProperties.PARTITION_WEIGHTS_SCOPE).entrySet().stream()
                .map(entry -> Map.entry((String) entry.getKey(), Double.valueOf((String) entry.getValue())))
                .collect(Collectors.toUnmodifiableMap(Entry::getKey, Entry::getValue));

        ThreadLocal<Reader<String>> readerThreadLocal = ThreadLocal.withInitial(() -> {
            Supplier<StreamingHasher> hasherFactory = StreamingHashersFactory.factoryForAlgorithm(hashAlgorithm);
            var spreader = new RendezvousStreamingSpreader(partitionWeights, hasherFactory);
            return new DestinationReader(
                    new EventReader(
                            ContainerReader.readTags(
                                    shardingKeys,
                                    new StreamingAlgorithmReader<>(new VariantReader(), Variant.ofNull(), spreader)
                            )
                    ),
                    spreader
            );
        });
        return new EnvelopDeserializer(readerThreadLocal::get);
    }

    /**
     * HTTP-server factory.
     *
     * @param properties       Application configuration properties.
     * @param metricsCollector Application {@link MetricsCollector} instance.
     * @return Created bean.
     */
    @NotNull
    UndertowHttpServer createHttpServer(Properties properties, MetricsCollector metricsCollector) {
        Properties serverProperties = PropertiesUtil.ofScope(properties, Scopes.HTTP_SERVER);
        serverProperties.setProperty(HttpServer.Props.IO_THREADS.name(), "1");
        serverProperties.setProperty(HttpServer.Props.WORKER_THREADS.name(), "1");

        RouteHandler handler = new InstrumentedRouteHandlerBuilder(serverProperties,
                metricsCollector)
                .get("/status", new KafkaStreamsStatusHttpHandler(kafkaStreamsState))
                .build();

        return new UndertowHttpServer(
                Application.application().getConfig().getHost(),
                Application.application().getConfig().getPort(),
                serverProperties,
                handler
        );
    }
}
