package ru.kontur.vostok.hercules.elastic.adapter.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.elastic.adapter.format.EventValidator;
import ru.kontur.vostok.hercules.elastic.adapter.format.JsonToEventFormatter;
import ru.kontur.vostok.hercules.gate.client.GateSender;
import ru.kontur.vostok.hercules.gate.client.GateStatus;
import ru.kontur.vostok.hercules.elastic.adapter.index.IndexManager;
import ru.kontur.vostok.hercules.elastic.adapter.index.IndexMeta;
import ru.kontur.vostok.hercules.http.ContentTypes;
import ru.kontur.vostok.hercules.http.HttpServerRequest;
import ru.kontur.vostok.hercules.http.HttpStatusCodes;
import ru.kontur.vostok.hercules.http.handler.HttpHandler;
import ru.kontur.vostok.hercules.json.Document;
import ru.kontur.vostok.hercules.json.DocumentReader;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.util.time.TimeUtil;

import java.util.Collections;

/**
 * Index single document using provided index name.
 * <p>
 * See Elasticsearch Index API docs for details.
 *
 * @author Gregory Koshelev
 */
public class IndexHandler implements HttpHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(IndexHandler.class);

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
                    tryComplete(request, HttpStatusCodes.INTERNAL_SERVER_ERROR);
                });
    }

    private void process(HttpServerRequest request, byte[] data, String index) {
        Document document = DocumentReader.read(data);
        if (document == null) {
            LOGGER.info("No document parsed from input");
            tryComplete(request, HttpStatusCodes.BAD_REQUEST, ContentTypes.TEXT_PLAIN_UTF_8, "No document parsed from input");
            return;
        }

        IndexMeta meta = indexManager.meta(index);
        if (meta == null) {
            tryComplete(request, HttpStatusCodes.NOT_FOUND, ContentTypes.TEXT_PLAIN_UTF_8,
                    "Index '" + index + "' is unknown");
            return;
        }

        String stream = meta.getStream();

        Event event = JsonToEventFormatter.format(document, index, meta);
        if (validator.validate(event)) {
            GateStatus status = gateSender.send(Collections.singletonList(event), false, stream);
            tryComplete(request, status);
        } else {
            LOGGER.info("Invalid document with timestamp " + TimeUtil.unixTicksToDateTime(event.getTimestamp()));
            tryComplete(request, HttpStatusCodes.BAD_REQUEST, ContentTypes.TEXT_PLAIN_UTF_8,
                    "Invalid document with timestamp " + TimeUtil.unixTicksToDateTime(event.getTimestamp()));
        }
    }

    private void tryComplete(HttpServerRequest request, GateStatus status) {
        switch (status) {
            case OK:
                tryComplete(request, HttpStatusCodes.OK);
                break;
            case BAD_REQUEST:
                tryComplete(request, HttpStatusCodes.INTERNAL_SERVER_ERROR);
            case GATE_UNAVAILABLE:
                tryComplete(request, HttpStatusCodes.SERVICE_UNAVAILABLE, ContentTypes.TEXT_PLAIN_UTF_8, "Gate is unavailable");
            default:
                tryComplete(request, HttpStatusCodes.INTERNAL_SERVER_ERROR);
        }
    }

    private void tryComplete(HttpServerRequest request, int code) {
        try {
            request.complete(code);
        } catch (Exception ex) {
            LOGGER.error("Error on request completion", ex);
        }
    }

    private void tryComplete(HttpServerRequest request, int code, String contentType, String data) {
        try {
            request.complete(code, contentType, data);
        } catch (Exception ex) {
            LOGGER.error("Error on request completion", ex);
        }
    }
}
