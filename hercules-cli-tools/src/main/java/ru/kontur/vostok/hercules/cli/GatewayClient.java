package ru.kontur.vostok.hercules.cli;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.Type;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.protocol.encoder.Encoder;
import ru.kontur.vostok.hercules.protocol.encoder.EventBuilder;
import ru.kontur.vostok.hercules.protocol.encoder.EventWriter;
import ru.kontur.vostok.hercules.util.args.ArgsParser;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;

import java.util.Map;
import java.util.Properties;

public class GatewayClient {

    private static String server;

    public static void main(String[] args) throws Exception {

        Map<String, String> parameters = ArgsParser.parse(args);
        Properties properties = PropertiesUtil.readProperties(parameters.getOrDefault("gateway-client.properties", "gateway-client.properties"));

        server = "http://" + properties.getProperty("server");

        EventBuilder eventBuilder = new EventBuilder();
        eventBuilder.setVersion(1);
        eventBuilder.setTimestamp(System.currentTimeMillis());
        eventBuilder.setTag("sample-tag", new Variant(Type.STRING, "sample value"));
        eventBuilder.setTag("sample-long", new Variant(Type.LONG, 123L));
        eventBuilder.setTag("sample-flag", new Variant(Type.FLAG, true));
        eventBuilder.setTag("sample-float", new Variant(Type.FLOAT, 0.123456789f));
        eventBuilder.setTag("sample-double", new Variant(Type.DOUBLE, 0.123456789));

        sendSingleEvent("test-elastic-sink", eventBuilder.build());

        Unirest.shutdown();
    }

    private static void sendSingleEvent(String streamName, Event event) throws Exception {

        Encoder encoder = new Encoder();
        encoder.writeInteger(1); // count
        EventWriter.write(encoder, event);

        HttpResponse<String> response = Unirest.post(server + "/stream/send")
                .queryString("stream", streamName)
                .header("apiKey", "test")
                .body(encoder.getBytes())
                .asString();

        System.out.println(response.getStatusText());
    }
}
