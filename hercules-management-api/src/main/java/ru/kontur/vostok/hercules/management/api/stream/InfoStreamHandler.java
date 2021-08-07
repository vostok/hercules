package ru.kontur.vostok.hercules.management.api.stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.auth.AuthProvider;
import ru.kontur.vostok.hercules.auth.AuthResult;
import ru.kontur.vostok.hercules.auth.AuthUtil;
import ru.kontur.vostok.hercules.auth.Right;
import ru.kontur.vostok.hercules.curator.exception.CuratorException;
import ru.kontur.vostok.hercules.http.HttpServerRequest;
import ru.kontur.vostok.hercules.http.HttpStatusCodes;
import ru.kontur.vostok.hercules.http.handler.HttpHandler;
import ru.kontur.vostok.hercules.http.query.QueryUtil;
import ru.kontur.vostok.hercules.management.api.QueryParameters;
import ru.kontur.vostok.hercules.meta.serialization.DeserializationException;
import ru.kontur.vostok.hercules.meta.stream.Stream;
import ru.kontur.vostok.hercules.meta.stream.StreamRepository;
import ru.kontur.vostok.hercules.undertow.util.HttpResponseContentWriter;
import ru.kontur.vostok.hercules.util.parameter.Parameter;

import java.util.Optional;

/**
 * @author Vladimir Tsypaev
 */
public class InfoStreamHandler implements HttpHandler {
    private static Logger LOGGER = LoggerFactory.getLogger(InfoStreamHandler.class);

    private final AuthProvider authProvider;
    private final StreamRepository repository;

    public InfoStreamHandler(StreamRepository repository, AuthProvider authProvider) {
        this.repository = repository;
        this.authProvider = authProvider;
    }

    @Override
    public void handle(HttpServerRequest request) {
        Parameter<String>.ParameterValue streamName = QueryUtil.get(QueryParameters.STREAM, request);
        if (QueryUtil.tryCompleteRequestIfError(request, streamName)) {
            return;
        }

        AuthResult authResult = authProvider.authAny(request, streamName.get(), Right.READ, Right.MANAGE);
        if (AuthUtil.tryCompleteRequestIfUnsuccessfulAuth(request, authResult)) {
            return;
        }

        Stream stream;
        try {
            Optional<Stream> optionalStream = repository.read(streamName.get());
            if (!optionalStream.isPresent()) {
                request.complete(HttpStatusCodes.NOT_FOUND);
                return;
            }
            stream = optionalStream.get();
        } catch (CuratorException ex) {
            LOGGER.error("Curator exception when read Stream", ex);
            request.complete(HttpStatusCodes.INTERNAL_SERVER_ERROR);
            return;
        } catch (DeserializationException ex) {
            LOGGER.error("Deserialization exception of Stream", ex);
            request.complete(HttpStatusCodes.INTERNAL_SERVER_ERROR);
            return;
        }

        HttpResponseContentWriter.writeJson(stream, request);
    }
}
