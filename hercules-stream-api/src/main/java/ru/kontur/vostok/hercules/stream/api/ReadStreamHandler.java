package ru.kontur.vostok.hercules.stream.api;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import ru.kontur.vostok.hercules.protocol.ByteStreamContent;
import ru.kontur.vostok.hercules.protocol.decoder.Decoder;
import ru.kontur.vostok.hercules.protocol.decoder.StreamReadStateReader;
import ru.kontur.vostok.hercules.protocol.encoder.ByteStreamContentWriter;
import ru.kontur.vostok.hercules.protocol.encoder.Encoder;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.Deque;
import java.util.Map;

public class ReadStreamHandler implements HttpHandler {

    private static final StreamReadStateReader stateReader = new StreamReadStateReader();
    private static final ByteStreamContentWriter contentWriter = new ByteStreamContentWriter();

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

                    ByteStreamContent streamContent = streamReader.getStreamContent(
                            streamName,
                            stateReader.read(new Decoder(message)),
                            k,
                            n,
                            take
                    );

                    exchange.getResponseHeaders().add(Headers.CONTENT_TYPE, OCTET_STREAM);

                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    Encoder encoder = new Encoder(stream);
                    contentWriter.write(encoder, streamContent);
                    exchange.getResponseSender().send(ByteBuffer.wrap(stream.toByteArray()));
                } catch (Exception e) {
                    e.printStackTrace();
                    exchange.setStatusCode(500);
                    exchange.endExchange();
                }
                finally {
                }
            });
        });
    }
}
