package ru.kontur.vostok.hercules.elastic.adapter.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.elastic.adapter.document.DocumentReader;
import ru.kontur.vostok.hercules.elastic.adapter.event.EventValidator;
import ru.kontur.vostok.hercules.elastic.adapter.event.LogEventMapper;
import ru.kontur.vostok.hercules.elastic.adapter.gate.GateSender;
import ru.kontur.vostok.hercules.elastic.adapter.gate.GateStatus;
import ru.kontur.vostok.hercules.elastic.adapter.index.IndexManager;
import ru.kontur.vostok.hercules.elastic.adapter.index.IndexMeta;
import ru.kontur.vostok.hercules.http.HttpServerRequest;
import ru.kontur.vostok.hercules.http.HttpStatusCodes;
import ru.kontur.vostok.hercules.http.handler.HttpHandler;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.util.functional.Result;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * @author Gregory Koshelev
 */
public class IndexHandler implements HttpHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(IndexHandler.class);
    private static final Map<GateStatus, Integer> statusCodes;

    static {
        Map<GateStatus, Integer> map = new EnumMap<>(GateStatus.class);
        map.put(GateStatus.OK, HttpStatusCodes.OK);
        map.put(GateStatus.BAD_REQUEST, HttpStatusCodes.INTERNAL_SERVER_ERROR);
        map.put(GateStatus.GATE_UNAVAILABLE, HttpStatusCodes.SERVICE_UNAVAILABLE);

        statusCodes = map;
    }

    private final IndexManager indexManager;
    private final GateSender gateSender;

    private final EventValidator validator;

    public IndexHandler(IndexManager indexManager, GateSender gateSender) {
        this.indexManager = indexManager;
        this.gateSender = gateSender;

        validator = new EventValidator();
    }

    @Override
    public void handle(HttpServerRequest request) {
        final String index = request.getPathParameter("index");

        request.readBodyAsync(
                (r, bytes) -> request.dispatchAsync(() -> {
                    try {
                        process(request, bytes, index);
                    } catch (Throwable throwable) {
                        LOGGER.error("Unknown error", throwable);
                        tryComplete(request, HttpStatusCodes.INTERNAL_SERVER_ERROR);
                    }
                }),
                (r, exception) -> {
                    LOGGER.error("Read body failed with exception", exception);
                    request.complete(HttpStatusCodes.INTERNAL_SERVER_ERROR);
                });
    }

    private void process(HttpServerRequest request, byte[] data, String index) {
        Map<String, Object> document = DocumentReader.read(data);
        if (document == null) {
            tryComplete(request, HttpStatusCodes.BAD_REQUEST);
        }

        IndexMeta indexMeta = indexManager.meta(index);
        if (indexMeta == null) {
            tryComplete(request, HttpStatusCodes.NOT_FOUND);
            return;
        }

        String stream = indexMeta.getStream();

        Result<Event, String> result = LogEventMapper.from(document, indexMeta.getProperties(), index);
        if (result.isOk() && validator.validate(result.get())) {
            GateStatus status = gateSender.send(Collections.singletonList(result.get()), false, stream);
            tryComplete(request, status);
        } else {
            tryComplete(request, HttpStatusCodes.BAD_REQUEST);
        }
    }

    private void tryComplete(HttpServerRequest request, GateStatus status) {
        int code = statusCodes.getOrDefault(status, HttpStatusCodes.INTERNAL_SERVER_ERROR);
        tryComplete(request, code);
    }

    private void tryComplete(HttpServerRequest request, int code) {
        try {
            request.complete(code);
        } catch (Exception ex) {
            LOGGER.error("Error on request completion", ex);
        }
    }
}
