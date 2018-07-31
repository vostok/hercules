package ru.kontur.vostok.hercules.cli;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.TimelineContent;
import ru.kontur.vostok.hercules.protocol.TimelineReadState;
import ru.kontur.vostok.hercules.protocol.TimelineShardReadState;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.protocol.decoder.Decoder;
import ru.kontur.vostok.hercules.protocol.decoder.EventReader;
import ru.kontur.vostok.hercules.protocol.decoder.TimelineContentReader;
import ru.kontur.vostok.hercules.protocol.encoder.Encoder;
import ru.kontur.vostok.hercules.protocol.encoder.TimelineReadStateWriter;
import ru.kontur.vostok.hercules.util.args.ArgsParser;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class TimelineApiClient {

    private static final TimelineReadStateWriter stateWriter = new TimelineReadStateWriter();
    private static final TimelineContentReader contentReader = new TimelineContentReader(EventReader.readAllTags());

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

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        Encoder encoder = new Encoder(stream);
        stateWriter.write(encoder, new TimelineReadState(new TimelineShardReadState[]{
        }));

        HttpResponse<InputStream> response = Unirest.post(server + "/timeline/read")
                .queryString("timeline", timelineName)
                .queryString("take", take)
                .queryString("k", 0)
                .queryString("n", 1)
                .queryString("from", 1530184600000L)
                .queryString("to", 1530184800000L)
                .body(stream.toByteArray())
                .asBinary();

        if (200 != response.getStatus()) {
            System.out.println(response.getStatusText());
            throw new Exception("Server error!");
        }

        byte[] buffer = new byte[response.getBody().available()];
        response.getBody().read(buffer);
        Decoder decoder = new Decoder(buffer);

        TimelineContent content = contentReader.read(decoder);

        System.out.println(String.format("Shard count: %d", content.getReadState().getShards().length));
        for (TimelineShardReadState shardReadState : content.getReadState().getShards()) {
            System.out.println(String.format("> Partition %d, tt_offset %d", shardReadState.getShardId(), shardReadState.getTtOffset()));
            System.out.println(String.format("> Event id: %s", shardReadState.getEventId()));
        }
        System.out.println("Content:");
        for (Event event : content.getEvents()) {
            System.out.println("> " + formatEvent(event));
        }
    }

    private static String formatEvent(Event event) {
        String tags = StreamSupport.stream(event.spliterator(), false)
                .map(e -> e.getKey() + "=" + formatVariant(e.getValue()))
                .collect(Collectors.joining(","));
        return String.format("(v. %d) [%s] %s", event.getVersion(), event.getId().toString(), tags);
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