package ru.kontur.vostok.hercules.elastic.adapter.gate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.gate.client.GateClient;
import ru.kontur.vostok.hercules.gate.client.exception.BadRequestException;
import ru.kontur.vostok.hercules.gate.client.exception.UnavailableClusterException;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.util.EventUtil;
import ru.kontur.vostok.hercules.util.concurrent.Topology;
import ru.kontur.vostok.hercules.util.parameter.Parameter;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * @author Gregory Koshelev
 */
public class GateSender {
    private static final Logger LOGGER = LoggerFactory.getLogger(GateSender.class);

    private final String apiKey;
    private final String[] urls;
    private final GateClient client;

    public GateSender(Properties properties) {
        this.apiKey = PropertiesUtil.get(Props.API_KEY, properties).get();
        this.urls = PropertiesUtil.get(Props.URLS, properties).get();
        Topology<String> whiteList = new Topology<>(urls);
        this.client = new GateClient(properties, whiteList);
    }

    public GateStatus send(List<Event> events, boolean async, String stream) {
        byte[] data = EventUtil.toBytes(events);

        try {
            if (async) {
                client.sendAsync(apiKey, stream, data);
            } else {
                client.send(apiKey, stream, data);
            }
            return GateStatus.OK;
        } catch (BadRequestException e) {
            LOGGER.error("Got exception from Gate", e);
            return GateStatus.BAD_REQUEST;
        } catch (UnavailableClusterException e) {
            LOGGER.error("No one of addresses is available." + Arrays.toString(urls));
            return GateStatus.GATE_UNAVAILABLE;
        }
    }

    public void close() {
        client.close();
    }

    private static class Props {
        static final Parameter<String[]> URLS =
                Parameter.stringArrayParameter("urls").
                        required().
                        build();

        static final Parameter<String> API_KEY =
                Parameter.stringParameter("apiKey").
                        required().
                        build();
    }

}
