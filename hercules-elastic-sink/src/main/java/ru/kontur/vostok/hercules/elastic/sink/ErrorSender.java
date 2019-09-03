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
import ru.kontur.vostok.hercules.util.concurrent.Topology;
import ru.kontur.vostok.hercules.util.parameter.Parameter;
import ru.kontur.vostok.hercules.util.parameter.parsing.Parsers;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class ErrorSender {

    private static final Logger LOGGER = LoggerFactory.getLogger(ErrorSender.class);

    private static final String COUNT_OF_EVENTS_SENDING_METER_KEY = "nonRetryableEventsSendingToGate";
    private static final String COUNT_OF_EVENTS_SENDING_WITH_ERROR_METER_KEY = "nonRetryableEventsSendingToGateWithError";

    private final String leproseryStream;
    private final GateClient gateClient;
    private final String leproseryApiKey;
    private final String[] urls;

    private final MetricsCollector metricsCollector;

    ErrorSender(Properties properties, MetricsCollector metricsCollector) {
        this.metricsCollector = metricsCollector;

        Properties leproseryProperties = PropertiesUtil.ofScope(properties, Scopes.LEPROSERY);
        this.leproseryStream = PropertiesUtil.get(Props.LEPROSERY_STREAM, leproseryProperties).get();
        this.leproseryApiKey = PropertiesUtil.get(Props.LEPROSERY_API_KEY, leproseryProperties).get();

        Properties gateProperties = PropertiesUtil.ofScope(properties, Scopes.GATE_CLIENT);
        this.urls = PropertiesUtil.get(Props.URLS, gateProperties).get();
        Topology<String> whiteList = new Topology<>(urls);
        this.gateClient = new GateClient(gateProperties, whiteList);
    }

    void sendErrors(List<Event> events) {
        int count = events.size();
        byte[] data = EventWriterUtil.toBytes(events.toArray(new Event[events.size()]));
        try {
            gateClient.send(leproseryApiKey, leproseryStream, data);
            metricsCollector.meter(COUNT_OF_EVENTS_SENDING_METER_KEY).mark(count);
        } catch (BadRequestException e) {
            LOGGER.error("Got exception from Gate", e);
            metricsCollector.meter(COUNT_OF_EVENTS_SENDING_WITH_ERROR_METER_KEY).mark(count);
        } catch (UnavailableClusterException e) {
            LOGGER.error("No one of addresses is available." + Arrays.toString(this.urls));
            metricsCollector.meter(COUNT_OF_EVENTS_SENDING_WITH_ERROR_METER_KEY).mark(count);
        }
    }

    private static class Props {
        static final Parameter<String> LEPROSERY_STREAM = Parameter
                .stringParameter("stream")
                .build();

        static final Parameter<String> LEPROSERY_API_KEY = Parameter
                .stringParameter("apiKey")
                .build();

        static final Parameter<String[]> URLS = Parameter
                .parameter("urls", Parsers.fromFunction(s -> s.split(",")))
                .build();
    }

}
