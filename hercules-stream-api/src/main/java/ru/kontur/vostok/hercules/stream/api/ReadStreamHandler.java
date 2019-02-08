package ru.kontur.vostok.hercules.stream.api;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.auth.AuthManager;
import ru.kontur.vostok.hercules.auth.AuthResult;
import ru.kontur.vostok.hercules.protocol.ByteStreamContent;
import ru.kontur.vostok.hercules.protocol.decoder.Decoder;
import ru.kontur.vostok.hercules.protocol.decoder.StreamReadStateReader;
import ru.kontur.vostok.hercules.protocol.encoder.ByteStreamContentWriter;
import ru.kontur.vostok.hercules.protocol.encoder.Encoder;
import ru.kontur.vostok.hercules.undertow.util.ExchangeUtil;
import ru.kontur.vostok.hercules.undertow.util.ResponseUtil;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.Deque;
import java.util.Map;
import java.util.Optional;

public class ReadStreamHandler implements HttpHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReadStreamHandler.class);

    private static final StreamReadStateReader STATE_READER = new StreamReadStateReader();
    private static final ByteStreamContentWriter CONTENT_WRITER = new ByteStreamContentWriter();

    private static final String OCTET_STREAM = "application/octet-stream";

    private final AuthManager authManager;
    private final StreamReader streamReader;

    public ReadStreamHandler(StreamReader streamReader, AuthManager authManager) {
        this.streamReader = streamReader;
        this.authManager = authManager;
    }

    @Override
    public void handleRequest(HttpServerExchange httpServerExchange) throws Exception {

        httpServerExchange.getRequestReceiver().receiveFullBytes((exchange, message) -> {
            exchange.dispatch(() -> {
                try {
                    Optional<String> optionalApiKey = ExchangeUtil.extractHeaderValue(exchange, "apiKey");
                    if (!optionalApiKey.isPresent()) {
                        ResponseUtil.unauthorized(exchange);
                        return;
                    }

                    Map<String, Deque<String>> queryParameters = exchange.getQueryParameters();
                    String streamName = queryParameters.get("stream").getFirst();

                    AuthResult authResult = authManager.authRead(optionalApiKey.get(),streamName);

                    if (!authResult.isSuccess()) {
                        if (authResult.isUnknown()) {
                            ResponseUtil.unauthorized(exchange);
                            return;
                        }
                        ResponseUtil.forbidden(exchange);
                        return;
                    }

                    int shardIndex = Integer.valueOf(queryParameters.get("shardIndex").getFirst());
                    int shardCount = Integer.valueOf(queryParameters.get("shardCount").getFirst());
                    int take = Integer.valueOf(queryParameters.get("take").getFirst());

                    ByteStreamContent streamContent = streamReader.getStreamContent(
                            streamName,
                            STATE_READER.read(new Decoder(message)),
                            shardIndex,
                            shardCount,
                            take
                    );

                    exchange.getResponseHeaders().add(Headers.CONTENT_TYPE, OCTET_STREAM);

                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    Encoder encoder = new Encoder(stream);
                    CONTENT_WRITER.write(encoder, streamContent);
                    exchange.getResponseSender().send(ByteBuffer.wrap(stream.toByteArray()));
                } catch (IllegalArgumentException e) {
                    ResponseUtil.badRequest(exchange);
                } catch (Exception e) {
                    LOGGER.error("Error on processing request", e);
                    ResponseUtil.internalServerError(exchange);
                }
            });
        });
    }
}
