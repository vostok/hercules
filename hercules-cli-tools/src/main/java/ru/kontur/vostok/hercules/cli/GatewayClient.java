package ru.kontur.vostok.hercules.cli;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import ru.kontur.vostok.hercules.protocol.*;
import ru.kontur.vostok.hercules.protocol.decoder.Decoder;
import ru.kontur.vostok.hercules.protocol.decoder.EventStreamContentReader;
import ru.kontur.vostok.hercules.protocol.encoder.Encoder;
import ru.kontur.vostok.hercules.protocol.encoder.EventBuilder;
import ru.kontur.vostok.hercules.protocol.encoder.EventWriter;
import ru.kontur.vostok.hercules.protocol.encoder.StreamReadStateWriter;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class GatewayClient {

    private static final String HOST = "http://localhost:6306/";

    public static void main(String[] args) throws Exception {

        EventBuilder eventBuilder = new EventBuilder();
        eventBuilder.setVersion(1);
        eventBuilder.setTimestamp(System.currentTimeMillis());
        eventBuilder.setTag("sample-tag", new Variant(Type.STRING, "sample value"));
        eventBuilder.setTag("sample-long", new Variant(Type.LONG, 123L));

        sendSingleEvent("gateway-test", eventBuilder.build());

        Unirest.shutdown();
    }

    private static void sendSingleEvent(String streamName, Event event) throws Exception {

        Encoder encoder = new Encoder();
        encoder.writeInteger(1); // count
        EventWriter.write(encoder, event);

        HttpResponse<String> response = Unirest.post(HOST + "/stream/send")
                .queryString("stream", streamName)
                .header("apiKey", "test")
                .body(encoder.getBytes())
                .asString();

        System.out.println(response.getStatusText());
    }




}
