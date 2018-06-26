package ru.kontur.vostok.hercules.cli;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.EventId;
import ru.kontur.vostok.hercules.protocol.TimelineReadState;
import ru.kontur.vostok.hercules.protocol.TimelineShardReadState;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.protocol.decoder.ArrrayReader;
import ru.kontur.vostok.hercules.protocol.decoder.Decoder;
import ru.kontur.vostok.hercules.protocol.decoder.EventReader2;
import ru.kontur.vostok.hercules.protocol.decoder.TimelineReadStateReader;
import ru.kontur.vostok.hercules.protocol.encoder.Encoder;
import ru.kontur.vostok.hercules.protocol.encoder.TimelineReadStateWriter;
import ru.kontur.vostok.hercules.util.args.ArgsParser;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.stream.Collectors;

public class TimelineApiClient {

    private static final TimelineReadStateWriter TIMELINE_READ_STATE_WRITER = new TimelineReadStateWriter();

    private static final TimelineReadStateReader TIMELINE_READ_STATE_READER = new TimelineReadStateReader();
    private static final ArrrayReader<Event> EVENT_ARRRAY_READER = new ArrrayReader<>(EventReader2.readAllTags(), Event.class);

    private static String server;

    public static void main(String[] args) throws Exception {
        Map<String, String> parameters = ArgsParser.parse(args);
        Properties properties = PropertiesUtil.readProperties(parameters.getOrDefault("timeline-api-client.properties", "timeline-api-client.properties"));

        server = "http://" + properties.getProperty("server");

        getStreamContent("timeline", 3);

        Unirest.shutdown();
    }

    private static void ping() throws Exception {

        HttpResponse<InputStream> response = Unirest.get(server + "/ping")
                .asBinary();

        System.out.println(response.getStatusText());
    }

    private static void getStreamContent(String timelineName, int take) throws Exception {

        Encoder encoder = new Encoder();
        TIMELINE_READ_STATE_WRITER.write(encoder, new TimelineReadState(new TimelineShardReadState[]{
                new TimelineShardReadState(0, 1, fromString("2d1cb070-7617-11e8-adc0-fa7ae01bbebc")),
                new TimelineShardReadState(1, 1, fromString("44517d82-7619-11e8-adc0-fa7ae01bbebc"))
        }));

        HttpResponse<InputStream> response = Unirest.post(server + "/timeline/read")
                .queryString("timeline", timelineName)
                .queryString("take", take)
                .queryString("k", 0)
                .queryString("n", 1)
                .queryString("from", 0)
                .queryString("to", 120000)
                .body(encoder.getBytes())
                .asBinary();

        if (200 != response.getStatus()) {
            System.out.println(response.getStatusText());
            throw new Exception("Server error!");
        }

        byte[] buffer = new byte[response.getBody().available()];
        response.getBody().read(buffer);
        Decoder decoder = new Decoder(buffer);

        TimelineReadState timelineReadState = TIMELINE_READ_STATE_READER.read(decoder);
        Event[] events = EVENT_ARRRAY_READER.read(decoder);

        System.out.println(String.format("Shard count: %d", timelineReadState.getShards().length));
        for (TimelineShardReadState shardReadState : timelineReadState.getShards()) {
            System.out.println(String.format("> Partition %d, timestamp %d", shardReadState.getShardId(), shardReadState.getEventTimestamp()));
            System.out.println(String.format("> Event id: %s", new UUID(shardReadState.getEventId().getP1(), shardReadState.getEventId().getP2())));
        }
        System.out.println("Content:");
        for (Event event : events) {
            System.out.println("> " + formatEvent(event));
        }
    }

    private static String formatEvent(Event event) {
        String tags = event.getTags().entrySet().stream()
                .map(e -> e.getKey() + "=" + formatVariant(e.getValue()))
                .collect(Collectors.joining(","));
        return String.format("(v. %d) [%d] %s", event.getVersion(), event.getTimestamp(), tags);
    }

    private static String formatVariant(Variant variant) {
        switch (variant.getType()) {
            case TEXT:
            case STRING:
                return new String((byte[]) variant.getValue(), StandardCharsets.UTF_8);
            default:
                return String.valueOf(variant.getValue());
        }
    }

    private static EventId fromString(String s) {
        UUID uuid = UUID.fromString(s);
        return new EventId(uuid.getMostSignificantBits(), uuid.getLeastSignificantBits());
    }
}
