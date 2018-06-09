package ru.kontur.vostok.hercules.cli;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import ru.kontur.vostok.hercules.protocol.*;
import ru.kontur.vostok.hercules.protocol.decoder.Decoder;
import ru.kontur.vostok.hercules.protocol.decoder.EventStreamContentReader;
import ru.kontur.vostok.hercules.protocol.encoder.Encoder;
import ru.kontur.vostok.hercules.protocol.encoder.StreamReadStateWriter;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class StreamApiClient {

    private static final String HOST = "http://localhost:6307/";

    public static void main(String[] args) throws Exception {

        getStreamContent("gateway-test", 10);

        Unirest.shutdown();
    }

    private static void getStreamContent(String streamName, int take) throws Exception {

        Encoder encoder = new Encoder();
        StreamReadStateWriter.write(encoder, new StreamReadState(new ShardReadState[]{
        }));

        HttpResponse<InputStream> response = Unirest.post(HOST + "/stream/read")
                .queryString("stream", streamName)
                .queryString("take", take)
                .queryString("k", 0)
                .queryString("n", 1)
                .body(encoder.getBytes())
                .asBinary();

        if (200 != response.getStatus()) {
            throw new Exception("Server error!");
        }

        byte[] buffer = new byte[response.getBody().available()];
        response.getBody().read(buffer);

        EventStreamContent eventStreamContent = EventStreamContentReader.read(new Decoder(buffer));
        StreamReadState readState = eventStreamContent.getState();

        System.out.println(String.format("Shard count: %d", readState.getShardCount()));
        for (ShardReadState shardReadState : readState.getShardStates()) {
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
