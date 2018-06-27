package ru.kontur.vostok.hercules.cli;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.EventStreamContent;
import ru.kontur.vostok.hercules.protocol.StreamReadState;
import ru.kontur.vostok.hercules.protocol.StreamShardReadState;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.protocol.decoder.Decoder;
import ru.kontur.vostok.hercules.protocol.decoder.EventStreamContentReader;
import ru.kontur.vostok.hercules.protocol.encoder.Encoder;
import ru.kontur.vostok.hercules.protocol.encoder.StreamReadStateWriter;
import ru.kontur.vostok.hercules.util.args.ArgsParser;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

public class StreamApiClient {

    private static final StreamReadStateWriter stateWriter = new StreamReadStateWriter();
    private static final EventStreamContentReader contentReader = new EventStreamContentReader();

    private static String server;

    public static void main(String[] args) throws Exception {
        Map<String, String> parameters = ArgsParser.parse(args);
        Properties properties = PropertiesUtil.readProperties(parameters.getOrDefault("stream-api-client.properties", "stream-api-client.properties"));

        server = "http://" + properties.getProperty("server");

        getStreamContent("test-elastic-sink", 10);

        Unirest.shutdown();
    }

    private static void getStreamContent(String streamName, int take) throws Exception {

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        Encoder encoder = new Encoder(stream);
        stateWriter.write(encoder, new StreamReadState(new StreamShardReadState[]{
        }));

        HttpResponse<InputStream> response = Unirest.post(server + "/stream/read")
                .queryString("stream", streamName)
                .queryString("take", take)
                .queryString("k", 0)
                .queryString("n", 1)
                .body(stream.toByteArray())
                .asBinary();

        if (200 != response.getStatus()) {
            throw new Exception("Server error!");
        }

        byte[] buffer = new byte[response.getBody().available()];
        response.getBody().read(buffer);

        EventStreamContent eventStreamContent = contentReader.read(new Decoder(buffer));
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
