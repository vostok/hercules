package ru.kontur.vostok.hercules.radamant;

import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KafkaStreams.State;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.processor.StreamPartitioner;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.application.Application;
import ru.kontur.vostok.hercules.application.ApplicationRunner;
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
import ru.kontur.vostok.hercules.kafka.util.serialization.EventDeserializer;
import ru.kontur.vostok.hercules.kafka.util.serialization.EventSerde;
import ru.kontur.vostok.hercules.kafka.util.serialization.EventSerializer;
import ru.kontur.vostok.hercules.kafka.util.serialization.UuidSerde;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.radamant.configurator.RuleFlatConfigurator;
import ru.kontur.vostok.hercules.radamant.rules.RuleTable;
import ru.kontur.vostok.hercules.radamant.rules.RuleTableFlat;
import ru.kontur.vostok.hercules.undertow.util.UndertowHttpServer;
import ru.kontur.vostok.hercules.util.lifecycle.Stoppable;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;

import java.time.Duration;
import java.util.Objects;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * Entry point for Radamant application
 * <p>
 * Application looks through the input stream,
 * selects events that satisfied one or more rules,
 * enriches and sends events to the output stream or streams.
 *
 * @author Tatyana Tokmyanina
 */
public class RadamantApplication implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(RadamantApplication.class);

    private final AtomicReference<State> kafkaStreamsState = new AtomicReference<>(State.CREATED);
    private RuleTable ruleTableFlat;
    private Enricher enricher;

    public static void main(String[] args) {
        Application.run(new RadamantApplication(), args);
    }

    @Override
    public String getApplicationId() {
        return "radamant";
    }

    @Override
    public String getApplicationName() {
        return "Hercules Radamant";
    }

    @Override
    public void init(Application application) {
        Container container = application.getContainer();
        Properties properties = application.getConfig().getAllProperties();

        MetricsCollector metricsCollector = createMetricsCollector(properties);
        container.register(metricsCollector);
        container.register(createKafkaStreams(properties, metricsCollector));
        container.register(createHttpServer(properties, metricsCollector));

        ruleTableFlat = new RuleTableFlat(metricsCollector);
        String rulesFlatConfigPath = PropertiesUtil
                .get(ConfigurationProperties.CONFIG_PATH, properties)
                .get();
        RuleFlatConfigurator ruleFlatConfigurator = new RuleFlatConfigurator(rulesFlatConfigPath);
        ruleFlatConfigurator.init(ruleTableFlat);
        setEnricher(new Enricher(metricsCollector));
    }

    /**
     * {@link MetricsCollector} factory.
     *
     * @param properties Application configuration properties.
     * @return Created bean.
     */
    @NotNull
    MetricsCollector createMetricsCollector(Properties properties) {
        MetricsCollector metricsCollector = new MetricsCollector(
                PropertiesUtil.ofScope(properties, Scopes.METRICS));
        CommonMetrics.registerCommonMetrics(metricsCollector);
        return metricsCollector;
    }

    /**
     * {@link KafkaStreams} factory.
     *
     * @param properties Application configuration properties.
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
                .build();
        Serde<UUID> idSerde = new UuidSerde();
        Serde<Event> eventSerde = new EventSerde(new EventSerializer(),
                EventDeserializer.parseAllTags());
        Consumed<UUID, Event> consumed = Consumed.with(idSerde, eventSerde);

        TopicNameGenerator nameGenerator = new TopicNameGenerator(PropertiesUtil.get(
                ConfigurationProperties.DEFAULT_RESULT_STREAM, properties).get());
        StreamPartitioner<UUID, Event> partitioner = new ByNewNameStreamPartitioner();

        LOGGER.debug(String.format(
                "Input stream: %s, output stream: %s",
                subscription,
                PropertiesUtil.get(ConfigurationProperties.DEFAULT_RESULT_STREAM, properties).get()
        ));

        var streamsBuilder = new StreamsBuilder();
        streamsBuilder
                .stream(subscription.toPattern(), consumed)
                .filter((id, event) -> MetricsUtil.isFlatMetric(event))
                .mapValues(event -> ruleTableFlat.navigate(event))
                .filter((id, ruleMatches) -> ruleMatches.getRuleMatches().size() > 0)
                .flatMapValues(ruleMatches -> ruleMatches.getRuleMatches().stream()
                        .map(ruleMatch -> enricher.enrich(ruleMatches.getEvent(), ruleMatch))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList()))
                .to(nameGenerator, Produced.with(idSerde, eventSerde, partitioner));
        return streamsBuilder.build();
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

        return new UndertowHttpServer(serverProperties, handler);
    }

    public void setRuleTableFlat(RuleTable ruleTableFlat) {
        this.ruleTableFlat = ruleTableFlat;
    }

    public void setEnricher(Enricher enricher) {
        this.enricher = enricher;
    }
}
