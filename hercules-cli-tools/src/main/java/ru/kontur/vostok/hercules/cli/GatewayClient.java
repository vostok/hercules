package ru.kontur.vostok.hercules.cli;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.protocol.encoder.Encoder;
import ru.kontur.vostok.hercules.protocol.encoder.EventBuilder;
import ru.kontur.vostok.hercules.protocol.encoder.EventWriter;
import ru.kontur.vostok.hercules.util.args.ArgsParser;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;
import ru.kontur.vostok.hercules.uuid.UuidGenerator;

import javax.xml.bind.DatatypeConverter;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;
import java.util.Random;

public class GatewayClient {

    private static final EventWriter eventWriter = new EventWriter();

    private static final Random RANDOM = new Random();

    private static String server;

    public static void main(String[] args) throws Exception {

        Map<String, String> parameters = ArgsParser.parse(args);
        Properties properties = PropertiesUtil.readProperties(parameters.getOrDefault("gateway-client.properties", "gateway-client.properties"));

        server = "http://" + properties.getProperty("server");

        sendEvents("test_sentry", generateEvents(1));

        Unirest.shutdown();
    }

    private static void sendSingleEvent(String streamName, Event event) throws Exception {

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        Encoder encoder = new Encoder(stream);
        encoder.writeInteger(1); // count
        eventWriter.write(encoder, event);

        HttpResponse<String> response = Unirest.post(server + "/stream/send")
                .queryString("stream", streamName)
                .header("apiKey", "test")
                .body(stream.toByteArray())
                .asString();

        System.out.println(response.getStatusText());
    }

    private static void sendEvents(String streamName, Event[] events) throws Exception {

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        Encoder encoder = new Encoder(stream);
        encoder.writeInteger(events.length);
        for (Event event : events) {
            eventWriter.write(encoder, event);
        }

        HttpResponse<String> response = Unirest.post(server + "/stream/send")
                .queryString("stream", streamName)
                .header("apiKey", "test")
                .body(stream.toByteArray())
                .asString();

        System.out.println(response.getStatusText());
    }

    private static Event[] generateEvents(int count) {
        Event[] events = new Event[count];
        for (int i = 0; i < count; ++i) {
            events[i] = generateEvent(i);
        }
        return events;
    }

    private static Event generateEvent(int i) {
        EventBuilder eventBuilder = new EventBuilder();
        eventBuilder.setVersion(1);
        eventBuilder.setEventId(UuidGenerator.getClientInstance().next());

        eventBuilder.setTag("sentry-token", Variant.ofString("1131f35ab5af49a1b1f3b1a3984d4590@4"));
        eventBuilder.setTag("platform", Variant.ofString("python"));
        eventBuilder.setTag("server", Variant.ofString("just.some.server"));

        eventBuilder.setTag("message", Variant.ofString("Try to use project name"));
        eventBuilder.setTag("environment", Variant.ofString("production"));
        eventBuilder.setTag("release", Variant.ofString("123.456"));

        eventBuilder.setTag("index", Variant.ofString("tstidx"));

        eventBuilder.setTag("metric-name", Variant.ofString("test.gateway.client"));
        //eventBuilder.setTag("metric-value", Variant.ofDouble(RANDOM.nextInt(100)));

        eventBuilder.setTag("exceptions", Variant.ofContainerVector(new Container[]{
                createException(new IllegalArgumentException("aaa"))
        }));
        eventBuilder.setTag("level", Variant.ofString("info"));
        Event result = eventBuilder.build();

        System.out.println("Event created: 0x" + DatatypeConverter.printHexBinary(result.getBytes()));
        return result;
    }

    private static Container createException(Throwable throwable) {

        LinkedList<Container> st = new LinkedList<>();

        for (StackTraceElement stackTraceElement : throwable.getStackTrace()) {
            Map<String, Variant> stackFrame0Map = new HashMap<>();
            stackFrame0Map.put("module", Variant.ofText(stackTraceElement.getClassName()));
            stackFrame0Map.put("function", Variant.ofString(stackTraceElement.getMethodName()));
            stackFrame0Map.put("filename", Variant.ofString(stackTraceElement.getFileName()));
            stackFrame0Map.put("lineno", Variant.ofInteger(stackTraceElement.getLineNumber()));
            stackFrame0Map.put("abs_path", Variant.ofText(stackTraceElement.getFileName()));
            st.add(new Container(stackFrame0Map));
        }

        Map<String, Variant> exceptionMap = new HashMap<>();
        exceptionMap.put("stacktrace", Variant.ofContainerArray(st.toArray(new Container[0])));
        exceptionMap.put("type", Variant.ofString(throwable.getClass().getSimpleName()));
        exceptionMap.put("value", Variant.ofText(throwable.getMessage()));
        exceptionMap.put("module", Variant.ofText(throwable.getClass().getPackage().getName()));

        return new Container(exceptionMap);
    }
}
