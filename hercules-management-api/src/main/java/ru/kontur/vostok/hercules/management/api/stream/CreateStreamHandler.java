package ru.kontur.vostok.hercules.management.api.stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import ru.kontur.vostok.hercules.auth.Action;
import ru.kontur.vostok.hercules.auth.AuthManager;
import ru.kontur.vostok.hercules.meta.curator.CreationResult;
import ru.kontur.vostok.hercules.meta.stream.Stream;
import ru.kontur.vostok.hercules.meta.stream.StreamRepository;
import ru.kontur.vostok.hercules.undertow.util.ExchangeUtil;
import ru.kontur.vostok.hercules.undertow.util.ResponseUtil;

import java.io.IOException;
import java.util.Optional;

/**
 * @author Gregory Koshelev
 */
public class CreateStreamHandler implements HttpHandler {
    private final AuthManager authManager;
    private final StreamRepository repository;

    private final ObjectReader deserializer;

    public CreateStreamHandler(AuthManager authManager, StreamRepository repository) {
        this.authManager = authManager;
        this.repository = repository;

        ObjectMapper objectMapper = new ObjectMapper();
        this.deserializer = objectMapper.readerFor(Stream.class);
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        Optional<String> optionalApiKey = ExchangeUtil.extractHeaderValue(exchange, "apiKey");
        if (!optionalApiKey.isPresent()) {
            ResponseUtil.unauthorized(exchange);
            return;
        }

        final String apiKey = optionalApiKey.get();
        exchange.getRequestReceiver().receiveFullBytes((exch, bytes) -> {
            try {
                Stream stream = deserializer.readValue(bytes);

                authManager.auth(apiKey, stream.getName(), Action.MANAGE);
                //TODO: Auth sources if needed

                CreationResult creationResult = repository.create(stream);
                if (!creationResult.isSuccess()) {
                    if (creationResult.getStatus() == CreationResult.Status.ALREADY_EXIST) {
                        ResponseUtil.conflict(exch);
                    } else {
                        ResponseUtil.internalServerError(exch);
                    }
                    return;
                }

                //TODO: create topic too
            } catch (IOException e) {
                e.printStackTrace();
                ResponseUtil.badRequest(exch);
                return;
            } catch (Exception e) {
                e.printStackTrace();
                ResponseUtil.internalServerError(exch);
                return;
            }
        }, (exch, exception) -> {
            ResponseUtil.badRequest(exch);
            return;
        });
    }
}
