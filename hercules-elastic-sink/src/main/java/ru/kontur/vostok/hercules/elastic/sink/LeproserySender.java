package ru.kontur.vostok.hercules.elastic.sink;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.application.Application;
import ru.kontur.vostok.hercules.configuration.Scopes;
import ru.kontur.vostok.hercules.gate.client.GateClient;
import ru.kontur.vostok.hercules.gate.client.exception.BadRequestException;
import ru.kontur.vostok.hercules.gate.client.exception.UnavailableClusterException;
import ru.kontur.vostok.hercules.gate.client.util.EventWriterUtil;
import ru.kontur.vostok.hercules.health.Meter;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.json.DocumentWriter;
import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.EventBuilder;
import ru.kontur.vostok.hercules.protocol.TinyString;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.sink.SinkContext;
import ru.kontur.vostok.hercules.tags.CommonTags;
import ru.kontur.vostok.hercules.tags.ElasticSearchTags;
import ru.kontur.vostok.hercules.tags.LogEventTags;
import ru.kontur.vostok.hercules.util.Lazy;
import ru.kontur.vostok.hercules.util.concurrent.Topology;
import ru.kontur.vostok.hercules.util.parameter.Parameter;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;
import ru.kontur.vostok.hercules.util.text.StringUtil;
import ru.kontur.vostok.hercules.util.time.TimeUtil;
import ru.kontur.vostok.hercules.util.validation.ValidationResult;
import ru.kontur.vostok.hercules.uuid.UuidGenerator;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * @author a.zhdanov
 */
class LeproserySender {
    private static final Logger LOGGER = LoggerFactory.getLogger(LeproserySender.class);

    private static final String PROJECT_NAME = "hercules";
    private static final String SERVICE_NAME = "hercules-elastic-sink";

    private static final TinyString SERVICE = TinyString.of("service");
    private static final TinyString TEXT = TinyString.of("text");
    private static final TinyString ORIGINAL_INDEX = TinyString.of("original-index");
    private static final TinyString ELASTIC_SINK_ID = TinyString.of("elastic-sink-id");
    private static final TinyString ELASTIC_SINK_GROUP_ID = TinyString.of("elastic-sink-groupId");
    private static final TinyString ELASTIC_SINK_SUBSCRIPTION = TinyString.of("elastic-sink-subscription");

    private static final int EMPTY_LEPROSERY_EVENT_SIZE_BYTES = 125;
    private static final int MAX_EVENT_SIZE_BYTES = 500_000;
    private static final int EXPECTED_EVENT_SIZE_BYTES = 2_048;

    private final String leproseryStream;
    private final String leproseryIndex;
    private final GateClient gateClient;
    private final String leproseryApiKey;
    private final String sinkId;
    private final Lazy<String> sinkGroupId;
    private final Lazy<String> sinkSubscription;

    private final Meter sentToLeproseryEventCountMeter;
    private final Meter sentToLeproseryWithErrorsEventCountMeter;

    LeproserySender(Properties properties, MetricsCollector metricsCollector) {
        sentToLeproseryEventCountMeter = metricsCollector.meter("sentToLeproseryEventCount");
        sentToLeproseryWithErrorsEventCountMeter = metricsCollector.meter("sentToLeproseryWithErrorsEventCount");

        this.leproseryStream = PropertiesUtil.get(Props.LEPROSERY_STREAM, properties).get();
        this.leproseryIndex = PropertiesUtil.get(Props.LEPROSERY_INDEX, properties).get();
        this.leproseryApiKey = PropertiesUtil.get(Props.LEPROSERY_API_KEY, properties).get();
        this.sinkId = StringUtil.getOrDefault(Application.context().getInstanceId(), "null");
        this.sinkGroupId = new Lazy<>(() -> StringUtil.getOrDefault(Application.context().get(SinkContext.GROUP_ID), "null"));
        this.sinkSubscription = new Lazy<>(() -> StringUtil.getOrDefault(Application.context().get(SinkContext.SUBSCRIPTION), "null"));

        Properties gateProperties = PropertiesUtil.ofScope(properties, Scopes.GATE_CLIENT);
        final String[] urls = PropertiesUtil.get(Props.URLS, gateProperties).get();
        Topology<String> whiteList = new Topology<>(urls);
        this.gateClient = new GateClient(gateProperties, whiteList);
    }

    /**
     * Convert event to leprosery like event and send to Leprosery.
     * @param eventErrorInfos  - map of event wrapper (contains event, event-index, event-id) -> result of validation with reason
     */
    public void convertAndSend(Map<ElasticDocument, ValidationResult> eventErrorInfos) {
        List<Event> events = convert(eventErrorInfos);
        int count = events.size();
        byte[] data = EventWriterUtil.toBytes(events.toArray(new Event[0]));
        try {
            gateClient.send(leproseryApiKey, leproseryStream, data);

            LOGGER.info("Send to leprosery {} events", count);
            sentToLeproseryEventCountMeter.mark(count);
            sentToLeproseryWithErrorsEventCountMeter.mark(eventErrorInfos.size() - count);
        } catch (BadRequestException | UnavailableClusterException e) {
            LOGGER.error("Leprosery sending error", e);
            sentToLeproseryWithErrorsEventCountMeter.mark(count);
        }
    }

    private List<Event> convert(Map<ElasticDocument, ValidationResult> eventErrorInfos) {
        return eventErrorInfos.entrySet().stream()
                .map(entry -> {
                    ElasticDocument document = entry.getKey();
                    ValidationResult validationResult = entry.getValue();
                    return toLeproseryEvent(document, validationResult.error());
                })
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    /**
     * Create Hercules Protocol event for leprosery with json line of invalid event
     *
     * @param document non-retryable elastic document
     * @param error message of error
     * @return Hercules Protocol event which is sent to leprosery
     */
    public Optional<Event> toLeproseryEvent(ElasticDocument document, String error) {
        int argsSize = (PROJECT_NAME + SERVICE_NAME + error + document.index() + leproseryIndex).getBytes(StandardCharsets.UTF_8).length;
        int minimalSize = EMPTY_LEPROSERY_EVENT_SIZE_BYTES + argsSize;
        int maxSize = MAX_EVENT_SIZE_BYTES - minimalSize;

        byte[] textBytes;
        try (ByteArrayOutputStream stream = new ByteArrayOutputStream(EXPECTED_EVENT_SIZE_BYTES)) {
            DocumentWriter.writeTo(stream, document.document());
            textBytes = stream.toByteArray();
        } catch (IOException e) {
            LOGGER.error("Error of creating json from non-retryable event for leprosery event", e);
            return Optional.empty();
        }

        if (textBytes.length > maxSize) {
            LOGGER.info("Leprosery message has invalid size ({}). The message will be truncated", textBytes.length);
            textBytes = Arrays.copyOfRange(textBytes, 0, maxSize);
        }

        return Optional.of(EventBuilder.create()
                .version(1)
                .timestamp(TimeUtil.dateTimeToUnixTicks(ZonedDateTime.now()))
                .uuid(UuidGenerator.getClientInstance().next())
                .tag(LogEventTags.MESSAGE_TAG.getName(), Variant.ofString(error))
                .tag(CommonTags.PROPERTIES_TAG.getName(), Variant.ofContainer(Container.builder()
                        .tag(CommonTags.PROJECT_TAG.getName(), Variant.ofString(PROJECT_NAME))
                        .tag(SERVICE, Variant.ofString(SERVICE_NAME))
                        .tag(TEXT, Variant.ofString(textBytes))
                        .tag(ORIGINAL_INDEX, Variant.ofString(document.index()))
                        .tag(ElasticSearchTags.ELK_INDEX_TAG.getName(), Variant.ofString(leproseryIndex))
                        .tag(ELASTIC_SINK_ID, Variant.ofString(sinkId))
                        .tag(ELASTIC_SINK_GROUP_ID, Variant.ofString(sinkGroupId.get()))
                        .tag(ELASTIC_SINK_SUBSCRIPTION, Variant.ofString(sinkSubscription.get()))
                        .build()))
                .build());
    }

    private static class Props {
        static final Parameter<String> LEPROSERY_STREAM = Parameter
                .stringParameter("stream")
                .required()
                .build();

        static final Parameter<String> LEPROSERY_API_KEY = Parameter
                .stringParameter("apiKey")
                .required()
                .build();

        static final Parameter<String[]> URLS = Parameter
                .stringArrayParameter("urls")
                .required()
                .build();

        static final Parameter<String> LEPROSERY_INDEX = Parameter
                .stringParameter("index")
                .withDefault("leprosery")
                .build();

    }

}
