package ru.kontur.vostok.hercules.cli;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import ru.kontur.vostok.hercules.protocol.ShardReadState;
import ru.kontur.vostok.hercules.protocol.StreamReadState;
import ru.kontur.vostok.hercules.protocol.decoder.Decoder;
import ru.kontur.vostok.hercules.protocol.decoder.StreamReadStateReader;

import java.io.InputStream;

public class StreamApiClient {

    private static final String HOST = "http://localhost:6306/";

    public static void main(String[] args) throws Exception {

        getStreamContent("stream-api-test", 10);


        Unirest.shutdown();
    }

    private static void getStreamContent(String streamName, int take) throws Exception {

        HttpResponse<String> response = Unirest.post(HOST + "/stream/read")
                .queryString("stream", streamName)
                .queryString("take", take)
                .queryString("k", 0)
                .queryString("n", 1)
                .asString();

        System.out.println(response.getBody());

/*
        if (200 != response.getStatus()) {
            throw new Exception("Server error!");
        }

        byte[] buffer = new byte[response.getBody().available()];
        response.getBody().read(buffer);

        StreamReadState readState = StreamReadStateReader.read(new Decoder(buffer));

        System.out.println(String.format("\nShard count: %d", readState.getShardCount()));
        for (ShardReadState shardReadState : readState.getShardStates()) {
            System.out.println(String.format("\n> Partition %d, offset %d", shardReadState.getPartition(), shardReadState.getOffset()));
        }*/

    }
}
