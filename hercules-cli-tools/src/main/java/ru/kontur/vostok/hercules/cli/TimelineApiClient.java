package ru.kontur.vostok.hercules.cli;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import ru.kontur.vostok.hercules.protocol.*;
import ru.kontur.vostok.hercules.protocol.decoder.*;
import ru.kontur.vostok.hercules.protocol.encoder.Encoder;
import ru.kontur.vostok.hercules.protocol.encoder.StreamReadStateWriter;
import ru.kontur.vostok.hercules.protocol.encoder.TimelineReadStateWriter;
import ru.kontur.vostok.hercules.util.args.ArgsParser;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

public class TimelineApiClient {

    private static final TimelineReadStateWriter TIMELINE_READ_STATE_WRITER = new TimelineReadStateWriter();

    private static final TimelineReadStateReader TIMELINE_READ_STATE_READER = new TimelineReadStateReader();
    private static final ArrrayReader<Event> EVENT_ARRRAY_READER = new ArrrayReader<>(EventReader., Event.class);
    //private static final EventStreamContentReader EVENT_STREAM_CONTENT_READER = new EventStreamContentReader();

    private static String server;

    public static void main(String[] args) throws Exception {
        Map<String, String> parameters = ArgsParser.parse(args);
        Properties properties = PropertiesUtil.readProperties(parameters.getOrDefault("timeline-api-client.properties", "timeline-api-client.properties"));

        server = "http://" + properties.getProperty("server");

        getStreamContent("balbal", 10);

        Unirest.shutdown();
    }

    private static void ping() throws Exception {

        HttpResponse<InputStream> response = Unirest.get(server + "/ping")
                .asBinary();

        System.out.println(response.getStatusText());
    }

    private static void getStreamContent(String streamName, int take) throws Exception {

        Encoder encoder = new Encoder();
        STREAM_READ_STATE_WRITER.write(encoder, new StreamReadState(new StreamShardReadState[]{
        }));

        HttpResponse<InputStream> response = Unirest.post(server + "/timeline/read")
                .queryString("stream", streamName)
                .queryString("take", take)
                .queryString("k", 0)
                .queryString("n", 1)
                .queryString("from", 1000)
                .queryString("to", 2000)
                .body(encoder.getBytes())
                .asBinary();

        if (200 != response.getStatus()) {
            throw new Exception("Server error!");
        }

        byte[] buffer = new byte[response.getBody().available()];
        response.getBody().read(buffer);

        EventStreamContent eventStreamContent = EVENT_STREAM_CONTENT_READER.read(new Decoder(buffer));
        StreamReadState readState = eventStreamContent.getState();

        System.out.println(String.format("Shard count: %d", readState.getShardCount()));
        for (StreamShardReadState shardReadState : readState.getShardStates()) {
            System.out.println(String.format("> Partition %d, offset %d", shardReadState.getPartition(), shardReadState.getOffset()));
        }
        System.out.println("Content:");
        for (Event event : eventStreamContent.getEvents()) {
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
}
