package ru.kontur.vostok.hercules.cli;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.protocol.encoder.Encoder;
import ru.kontur.vostok.hercules.protocol.encoder.EventBuilder;
import ru.kontur.vostok.hercules.protocol.encoder.EventWriter;
import ru.kontur.vostok.hercules.util.args.ArgsParser;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;

import java.util.Map;
import java.util.Properties;

public class GatewayClient {

    private static final EventWriter EVENT_WRITER = new EventWriter();

    private static String server;

    public static void main(String[] args) throws Exception {

        Map<String, String> parameters = ArgsParser.parse(args);
        Properties properties = PropertiesUtil.readProperties(parameters.getOrDefault("gateway-client.properties", "gateway-client.properties"));

        server = "http://" + properties.getProperty("server");

/*
        for (int i = 0; i < 1000; ++i) {
            sendEvents("test-elastic-sink", generateEvents(10000));
        }*/

        //sendSingleEvent("test-elastic-sink", generateEvent());
        sendEvents("test-elastic-sink", generateEvents(10));

        Unirest.shutdown();
    }

    private static void sendSingleEvent(String streamName, Event event) throws Exception {

        Encoder encoder = new Encoder();
        encoder.writeInteger(1); // count
        EVENT_WRITER.write(encoder, event);

        HttpResponse<String> response = Unirest.post(server + "/stream/send")
                .queryString("stream", streamName)
                .header("apiKey", "test")
                .body(encoder.getBytes())
                .asString();

        System.out.println(response.getStatusText());
    }

    private static void sendEvents(String streamName, Event[] events) throws Exception {
        Encoder encoder = new Encoder();
        encoder.writeInteger(events.length);
        for (Event event : events) {
            EVENT_WRITER.write(encoder, event);
        }

        HttpResponse<String> response = Unirest.post(server + "/stream/send")
                .queryString("stream", streamName)
                .header("apiKey", "test")
                .body(encoder.getBytes())
                .asString();

        System.out.println(response.getStatusText());
    }

    private static Event[] generateEvents(int count) {
        Event[] events = new Event[count];
        for (int i = 0; i < count; ++i) {
            events[i] = generateEvent();
        }
        return events;
    }

    private static Event generateEvent() {
        EventBuilder eventBuilder = new EventBuilder();
        eventBuilder.setVersion(1);
        eventBuilder.setTimestamp(System.nanoTime());
        eventBuilder.setTag("sample-tag", Variant.ofString("sample value"));
        eventBuilder.setTag("sample-long", Variant.ofLong(123L));
        eventBuilder.setTag("sample-flag", Variant.ofFlag(true));
        eventBuilder.setTag("sample-float", Variant.ofFloat(0.123456789f));
        eventBuilder.setTag("sample-double", Variant.ofDouble(0.123456789));

        return eventBuilder.build();
    }
}
