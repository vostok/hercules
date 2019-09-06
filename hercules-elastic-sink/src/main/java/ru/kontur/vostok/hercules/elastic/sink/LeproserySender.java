package ru.kontur.vostok.hercules.elastic.sink;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.configuration.Scopes;
import ru.kontur.vostok.hercules.gate.client.GateClient;
import ru.kontur.vostok.hercules.gate.client.exception.BadRequestException;
import ru.kontur.vostok.hercules.gate.client.exception.UnavailableClusterException;
import ru.kontur.vostok.hercules.gate.client.util.EventWriterUtil;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.protocol.format.EventFormatter;
import ru.kontur.vostok.hercules.protocol.util.ContainerBuilder;
import ru.kontur.vostok.hercules.protocol.util.EventBuilder;
import ru.kontur.vostok.hercules.tags.CommonTags;
import ru.kontur.vostok.hercules.tags.ElasticSearchTags;
import ru.kontur.vostok.hercules.util.concurrent.Topology;
import ru.kontur.vostok.hercules.util.parameter.Parameter;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;
import ru.kontur.vostok.hercules.util.time.TimeUtil;
import ru.kontur.vostok.hercules.util.validation.ValidationResult;
import ru.kontur.vostok.hercules.uuid.UuidGenerator;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * @author a.zhdanov
 */
class LeproserySender {

    private static final Logger LOGGER = LoggerFactory.getLogger(LeproserySender.class);

    private static final String PROJECT_NAME = "hercules";
    private static final String SERVICE_NAME = "hercules-elastic-sink";

    private static final String COUNT_OF_EVENTS_SENDING_METER_KEY = "nonRetryableEventsSendingToGate";
    private static final String COUNT_OF_EVENTS_SENDING_WITH_ERROR_METER_KEY = "nonRetryableEventsSendingToGateWithError";

    private final String leproseryStream;
    private final String leproseryIndex;
    private final GateClient gateClient;
    private final String leproseryApiKey;
    private final String[] urls;

    private final MetricsCollector metricsCollector;

    LeproserySender(Properties properties, MetricsCollector metricsCollector) {
        this.metricsCollector = metricsCollector;

        Properties leproseryProperties = PropertiesUtil.ofScope(properties, Scopes.LEPROSERY);
        this.leproseryStream = PropertiesUtil.get(Props.LEPROSERY_STREAM, leproseryProperties).get();
        this.leproseryIndex = PropertiesUtil.get(Props.LEPROSERY_INDEX, properties).get();
        this.leproseryApiKey = PropertiesUtil.get(Props.LEPROSERY_API_KEY, leproseryProperties).get();

        Properties gateProperties = PropertiesUtil.ofScope(leproseryProperties, Scopes.GATE_CLIENT);
        this.urls = PropertiesUtil.get(Props.URLS, gateProperties).get();
        Topology<String> whiteList = new Topology<>(urls);
        this.gateClient = new GateClient(gateProperties, whiteList);
    }

    public void send(Map<EventWrapper, ValidationResult> validationResultMap) throws RuntimeException {
        send(
                validationResultMap.entrySet().stream()
                        .map(entry -> {
                            EventWrapper wrapper = entry.getKey();
                            ValidationResult validationResult = entry.getValue();
                            return toLeproseryEvent(wrapper.getEvent(), leproseryIndex, wrapper.getIndex(), validationResult.error());
                        })
                        .collect(Collectors.toList())
        );
    }

    public void send(List<Event> events) throws RuntimeException {
        int count = events.size();
        byte[] data = EventWriterUtil.toBytes(events.toArray(new Event[events.size()]));
        try {
            gateClient.send(leproseryApiKey, leproseryStream, data);

            LOGGER.info("Send to leprosery {} events", count);
            metricsCollector.meter(COUNT_OF_EVENTS_SENDING_METER_KEY).mark(count);
        } catch (BadRequestException e) {
            LOGGER.error("Got exception from Gate", e);
            metricsCollector.meter(COUNT_OF_EVENTS_SENDING_WITH_ERROR_METER_KEY).mark(count);

            throw new IllegalArgumentException("Sending to Leprosery is ERROR. Bad request");
        } catch (UnavailableClusterException e) {
            LOGGER.error("No one of addresses is available." + Arrays.toString(this.urls));
            metricsCollector.meter(COUNT_OF_EVENTS_SENDING_WITH_ERROR_METER_KEY).mark(count);

            throw new IllegalStateException("Sending to Leprosery is ERROR. Unavailable error");
        }
    }

    /**
     * Create Hercules Protocol event for leprosery with json line of invalid event
     *
     * @param event          non-retryable event
     * @param leproseryIndex name of destination index
     * @param index          to be used when put events to ELK
     * @param reason         message of error
     * @return Hercules Protocol event which is sent to leprosery
     */
    private Event toLeproseryEvent(Event event, String leproseryIndex, String index, String reason) {
        return EventBuilder.create()
                .version(1)
                .timestamp(TimeUtil.dateTimeToUnixTicks(ZonedDateTime.now()))
                .random(UuidGenerator.getClientInstance().next())
                .tag("message", Variant.ofString(reason))
                .tag(CommonTags.PROPERTIES_TAG, Variant.ofContainer(ContainerBuilder.create()
                        .tag(CommonTags.PROJECT_TAG, Variant.ofString(PROJECT_NAME))
                        .tag(CommonTags.SERVICE_TAG, Variant.ofString(SERVICE_NAME))
                        .tag("text", Variant.ofString(EventFormatter.format(event, true)))
                        .tag("original-index", Variant.ofString(index))
                        .tag(ElasticSearchTags.ELK_INDEX_TAG.getName(), Variant.ofString(leproseryIndex))
                        .build()))
                .build();
    }

    private static class Props {
        static final Parameter<String> LEPROSERY_STREAM = Parameter
                .stringParameter("stream")
                .build();

        static final Parameter<String> LEPROSERY_API_KEY = Parameter
                .stringParameter("apiKey")
                .build();

        static final Parameter<String[]> URLS = Parameter
                .stringArrayParameter("urls")
                .build();

        static final Parameter<String> LEPROSERY_INDEX = Parameter
                .stringParameter("index")
                .withDefault("leprosery")
                .build();

    }

}
