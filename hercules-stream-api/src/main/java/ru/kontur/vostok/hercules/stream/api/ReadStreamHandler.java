package ru.kontur.vostok.hercules.stream.api;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import org.apache.kafka.streams.StreamsBuilder;
import ru.kontur.vostok.hercules.protocol.EventStreamContent;
import ru.kontur.vostok.hercules.protocol.ShardReadState;
import ru.kontur.vostok.hercules.protocol.StreamReadState;

import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ReadStreamHandler implements HttpHandler {

    private final StreamReader streamReader;

    public ReadStreamHandler(StreamReader streamReader) {
        this.streamReader = streamReader;
    }

    @Override
    public void handleRequest(HttpServerExchange httpServerExchange) throws Exception {

        httpServerExchange.getResponseHeaders().add(Headers.CONTENT_TYPE, "text/plain");

        Map<String, Deque<String>> queryParameters = httpServerExchange.getQueryParameters();
        String streamName = queryParameters.get("stream").getFirst();
        int k = Integer.valueOf(queryParameters.get("k").getFirst());
        int n = Integer.valueOf(queryParameters.get("n").getFirst());
        int take = Integer.valueOf(queryParameters.get("take").getFirst());

        EventStreamContent streamContent = streamReader.getStreamContent(
                streamName,
                new StreamReadState(new ShardReadState[]{}),
                k,
                n,
                take
        );

        StringBuilder res = new StringBuilder();
        res.append("PartReadedCount: ").append(streamContent.getState().getShardCount()).append("\n");
        for (ShardReadState state : streamContent.getState().getShardStates()) {
            res.append("  shard: ").append(state.getPartition()).append(" offset:").append(state.getOffset()).append("\n");
        }
        res.append("Total events: ").append(streamContent.getEventCount()).append("\n");
        for (String event : streamContent.getEvents()) {
            res.append("> ").append(event).append("\n");
        }
        httpServerExchange.getResponseSender().send(res.toString());
    }
}
