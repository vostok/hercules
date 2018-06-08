package ru.kontur.vostok.hercules.stream.api;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import org.apache.kafka.streams.StreamsBuilder;
import ru.kontur.vostok.hercules.protocol.EventStreamContent;
import ru.kontur.vostok.hercules.protocol.ShardReadState;
import ru.kontur.vostok.hercules.protocol.StreamReadState;
import ru.kontur.vostok.hercules.protocol.decoder.Decoder;
import ru.kontur.vostok.hercules.protocol.decoder.StreamReadStateReader;
import ru.kontur.vostok.hercules.protocol.encoder.Encoder;
import ru.kontur.vostok.hercules.protocol.encoder.EventStreamContentWriter;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ReadStreamHandler implements HttpHandler {

    private static final String OCTET_STREAM = "application/octet-stream";

    private final StreamReader streamReader;

    public ReadStreamHandler(StreamReader streamReader) {
        this.streamReader = streamReader;
    }

    @Override
    public void handleRequest(HttpServerExchange httpServerExchange) throws Exception {

        httpServerExchange.getRequestReceiver().receiveFullBytes((exchange, message) -> {
            exchange.dispatch(() -> {
                try {
                    Map<String, Deque<String>> queryParameters = exchange.getQueryParameters();
                    String streamName = queryParameters.get("stream").getFirst();
                    int k = Integer.valueOf(queryParameters.get("k").getFirst());
                    int n = Integer.valueOf(queryParameters.get("n").getFirst());
                    int take = Integer.valueOf(queryParameters.get("take").getFirst());

                    EventStreamContent streamContent = streamReader.getStreamContent(
                            streamName,
                            StreamReadStateReader.read(new Decoder(message)),
                            k,
                            n,
                            take
                    );

                    exchange.getResponseHeaders().add(Headers.CONTENT_TYPE, OCTET_STREAM);

                    Encoder encoder = new Encoder();
                    EventStreamContentWriter.write(encoder, streamContent);
                    exchange.getResponseSender().send(ByteBuffer.wrap(encoder.getBytes()));
                } catch (Exception e) {
                    e.printStackTrace();
                    exchange.setStatusCode(500);
                }
                finally {
                    exchange.endExchange();
                }
            });
        });
    }
}
