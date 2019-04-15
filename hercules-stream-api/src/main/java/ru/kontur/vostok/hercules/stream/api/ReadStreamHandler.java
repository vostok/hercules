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
import ru.kontur.vostok.hercules.undertow.util.ContentTypes;
import ru.kontur.vostok.hercules.undertow.util.ExchangeUtil;
import ru.kontur.vostok.hercules.undertow.util.ResponseUtil;
import ru.kontur.vostok.hercules.util.functional.Result;
import ru.kontur.vostok.hercules.util.parsing.Parsers;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.Optional;

public class ReadStreamHandler implements HttpHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReadStreamHandler.class);

    private static final StreamReadStateReader STATE_READER = new StreamReadStateReader();
    private static final ByteStreamContentWriter CONTENT_WRITER = new ByteStreamContentWriter();

    private static final String REASON_MISSING_PARAM = "Missing required parameter ";

    private static final String PARAM_STREAM = "stream";
    private static final String PARAM_SHARD_INDEX = "shardIndex";
    private static final String PARAM_SHARD_COUNT = "shardCount";
    private static final String PARAM_TAKE = "take";

    private final AuthManager authManager;
    private final StreamReader streamReader;

    public ReadStreamHandler(StreamReader streamReader, AuthManager authManager) {
        this.streamReader = streamReader;
        this.authManager = authManager;
    }

    @Override
    public void handleRequest(HttpServerExchange httpServerExchange) throws Exception {

        Optional<Integer> optionalContentLength = ExchangeUtil.extractContentLength(httpServerExchange);
        if (!optionalContentLength.isPresent()) {
            ResponseUtil.lengthRequired(httpServerExchange);
            return;
        }
        if (optionalContentLength.get() < 0) {
            ResponseUtil.badRequest(httpServerExchange);
            return;
        }

        Optional<String> optionalApiKey = ExchangeUtil.extractHeaderValue(httpServerExchange, "apiKey");
        if (!optionalApiKey.isPresent()) {
            ResponseUtil.unauthorized(httpServerExchange);
            return;
        }

        Optional<String> optionalStreamName = ExchangeUtil.extractQueryParam(httpServerExchange, PARAM_STREAM);
        if (!optionalStreamName.isPresent()) {
            ResponseUtil.badRequest(httpServerExchange, REASON_MISSING_PARAM + PARAM_STREAM);
            return;
        }

        String apiKey = optionalApiKey.get();
        String streamName = optionalStreamName.get();

        AuthResult authResult = authManager.authRead(apiKey, streamName);

        if (!authResult.isSuccess()) {
            if (authResult.isUnknown()) {
                ResponseUtil.unauthorized(httpServerExchange);
                return;
            }
            ResponseUtil.forbidden(httpServerExchange);
            return;
        }

        Optional<String> optionalShardIndex = ExchangeUtil.extractQueryParam(httpServerExchange, PARAM_SHARD_INDEX);
        if (!optionalShardIndex.isPresent()) {
            ResponseUtil.badRequest(httpServerExchange, REASON_MISSING_PARAM + PARAM_SHARD_INDEX);
            return;
        }

        Result<Integer, String> shardIndex = Parsers.parseInteger(optionalShardIndex.get());
        if (!shardIndex.isOk()) {
            ResponseUtil.badRequest(httpServerExchange, shardIndex.getError() + " in parameter " + PARAM_SHARD_INDEX);
            return;
        }

        Optional<String> optionalShardCount = ExchangeUtil.extractQueryParam(httpServerExchange, PARAM_SHARD_COUNT);
        if (!optionalShardCount.isPresent()) {
            ResponseUtil.badRequest(httpServerExchange, REASON_MISSING_PARAM + PARAM_SHARD_COUNT);
            return;
        }

        Result<Integer, String> shardCount = Parsers.parseInteger(optionalShardCount.get());
        if (!shardCount.isOk()) {
            ResponseUtil.badRequest(httpServerExchange, shardCount.getError() + " in parameter " + PARAM_SHARD_COUNT);
            return;
        }

        Optional<String> optionalTake = ExchangeUtil.extractQueryParam(httpServerExchange, PARAM_TAKE);
        if (!optionalTake.isPresent()) {
            ResponseUtil.badRequest(httpServerExchange, REASON_MISSING_PARAM + PARAM_TAKE);
            return;
        }

        Result<Integer, String> take = Parsers.parseInteger(optionalTake.get());
        if (!take.isOk()) {
            ResponseUtil.badRequest(httpServerExchange, take.getError() + " in parameter " + PARAM_TAKE);
            return;
        }

        httpServerExchange.getRequestReceiver().receiveFullBytes((exchange, message) -> {
            exchange.dispatch(() -> {
                try {
                    ByteStreamContent streamContent = streamReader.getStreamContent(
                            streamName,
                            STATE_READER.read(new Decoder(message)),
                            shardIndex.get(),
                            shardCount.get(),
                            take.get()
                    );

                    exchange.getResponseHeaders().add(Headers.CONTENT_TYPE, ContentTypes.APPLICATION_OCTET_STREAM);

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
