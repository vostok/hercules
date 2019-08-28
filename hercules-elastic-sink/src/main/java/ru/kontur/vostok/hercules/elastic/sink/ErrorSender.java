package ru.kontur.vostok.hercules.elastic.sink;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.elastic.sink.ElasticResponseHandler.ErrorResponseWrapper;
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
import ru.kontur.vostok.hercules.protocol.util.EventUtil;
import ru.kontur.vostok.hercules.tags.CommonTags;
import ru.kontur.vostok.hercules.tags.ElasticSearchTags;
import ru.kontur.vostok.hercules.util.concurrent.Topology;
import ru.kontur.vostok.hercules.util.parsing.Parsers;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;
import ru.kontur.vostok.hercules.util.properties.PropertyDescription;
import ru.kontur.vostok.hercules.util.properties.PropertyDescriptionBuilder;
import ru.kontur.vostok.hercules.util.time.TimeUtil;
import ru.kontur.vostok.hercules.uuid.UuidGenerator;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

public class ErrorSender {

    private static final Logger LOGGER = LoggerFactory.getLogger(ErrorSender.class);
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

    ErrorSender(Properties properties, MetricsCollector metricsCollector) {
        Properties gateProperties = PropertiesUtil.ofScope(properties, "gate");

        this.leproseryStream = Props.LEPROSERY_STREAM.extract(gateProperties);
        this.leproseryIndex = Props.LEPROSERY_INDEX.extract(gateProperties);
        this.leproseryApiKey = Props.LEPROSERY_API_KEY.extract(gateProperties);
        this.metricsCollector = metricsCollector;

        this.urls = Props.URLS.extract(gateProperties);
        Topology<String> whiteList = new Topology<>(urls);
        this.gateClient = new GateClient(gateProperties, whiteList);
    }

    void sendNonRetryableEvents(List<Event> events, Set<ErrorResponseWrapper> errorResponseWrappers) {
        List<Event> nonRetyableEvents = extractAndWrap(events, errorResponseWrappers);
        byte[] data = EventWriterUtil.toBytes(nonRetyableEvents.toArray(new Event[events.size()]));
        try {
            gateClient.send(leproseryApiKey, leproseryStream, data);
            metricsCollector.meter(COUNT_OF_EVENTS_SENDING_METER_KEY).mark();
        } catch (BadRequestException e) {
            LOGGER.error("Got exception from Gate", e);
            metricsCollector.meter(COUNT_OF_EVENTS_SENDING_WITH_ERROR_METER_KEY).mark();
        } catch (UnavailableClusterException e) {
            LOGGER.error("No one of addresses is available." + Arrays.toString(this.urls));
            metricsCollector.meter(COUNT_OF_EVENTS_SENDING_WITH_ERROR_METER_KEY).mark();
        }
    }

    /**
     * Method extracts from all events only non-retryable events and wrap their to leprosery like event
     *
     * @param events                all input events
     * @param errorResponseWrappers wrapper of elastic error with event id, reason
     * @return list of wrapped non-retryable events
     */
    private List<Event> extractAndWrap(List<Event> events, Set<ErrorResponseWrapper> errorResponseWrappers) {
        Map<String, ErrorResponseWrapper> errorsMap = errorResponseWrappers.stream()
                .collect(Collectors.toMap(ErrorResponseWrapper::getEventId, errorResponseWrapper -> errorResponseWrapper));
        List<Event> wrappers = new ArrayList<>(events.size());
        for (Event event : events) {
            String id = EventUtil.extractStringId(event);
            ErrorResponseWrapper errorResponseWrapper = errorsMap.get(id);
            if (errorResponseWrapper != null) {
                String index = errorResponseWrapper.getIndex();
                String reason = errorResponseWrapper.getReason();
                Event leproseryEvent = toLeproseryEvent(event, index, reason);
                wrappers.add(leproseryEvent);
            }
        }
        return wrappers;
    }

    /**
     * Create Hercules Protocol event for leprosery with json line of invalid event
     *
     * @param event  non-retryable event
     * @param index  to be used when put events to ELK
     * @param reason message of error
     * @return Hercules Protocol event which is sent to leprosery
     */
    private Event toLeproseryEvent(Event event, String index, String reason) {
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
        static final PropertyDescription<String> LEPROSERY_STREAM = PropertyDescriptionBuilder
                .start("leproseryStream", String.class, Parsers::parseString)
                .build();

        static final PropertyDescription<String> LEPROSERY_INDEX = PropertyDescriptionBuilder
                .start("leproseryIndex", String.class, Parsers::parseString)
                .withDefaultValue("leprosery")
                .build();

        static final PropertyDescription<String> LEPROSERY_API_KEY = PropertyDescriptionBuilder
                .start("leproseryApiKey", String.class, Parsers::parseString)
                .build();

        static final PropertyDescription<String[]> URLS = PropertyDescriptionBuilder
                .start("urls", String[].class, Parsers.parseArray(String.class, Parsers::parseString))
                .build();
    }

}
