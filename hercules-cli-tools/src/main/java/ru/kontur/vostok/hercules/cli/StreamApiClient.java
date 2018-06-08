package ru.kontur.vostok.hercules.cli;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import ru.kontur.vostok.hercules.protocol.EventStreamContent;
import ru.kontur.vostok.hercules.protocol.ShardReadState;
import ru.kontur.vostok.hercules.protocol.StreamReadState;
import ru.kontur.vostok.hercules.protocol.decoder.Decoder;
import ru.kontur.vostok.hercules.protocol.decoder.EventStreamContentReader;
import ru.kontur.vostok.hercules.protocol.encoder.Encoder;
import ru.kontur.vostok.hercules.protocol.encoder.StreamReadStateWriter;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class StreamApiClient {

    private static final String HOST = "http://localhost:6306/";

    public static void main(String[] args) throws Exception {

        List<Thread> ts = new ArrayList<>();
        for (int i = 0; i < 100; ++i) {
            Thread t = new Thread(() -> {
                try {
                    while (true) {
                        getStreamContent("stream-api-test", 10);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            ts.add(t);
        }
        ts.forEach(Thread::start);
        ts.forEach(t -> {
            try {
                t.join();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });


        Unirest.shutdown();
    }

    private static void getStreamContent(String streamName, int take) throws Exception {

        Encoder encoder = new Encoder();
        StreamReadStateWriter.write(encoder, new StreamReadState(new ShardReadState[]{
                new ShardReadState(0, 1),
                new ShardReadState(1, 0)
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
        for (String event : eventStreamContent.getEvents()) {
            System.out.println("> " + event);
        }
    }
}
