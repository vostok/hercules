package ru.kontur.vostok.hercules.elastic.adapter.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.elastic.adapter.bulk.BulkReader;
import ru.kontur.vostok.hercules.elastic.adapter.bulk.IndexRequest;
import ru.kontur.vostok.hercules.elastic.adapter.format.EventValidator;
import ru.kontur.vostok.hercules.elastic.adapter.format.JsonToEventFormatter;
import ru.kontur.vostok.hercules.elastic.adapter.gate.GateSender;
import ru.kontur.vostok.hercules.elastic.adapter.gate.GateStatus;
import ru.kontur.vostok.hercules.elastic.adapter.index.IndexManager;
import ru.kontur.vostok.hercules.elastic.adapter.index.IndexMeta;
import ru.kontur.vostok.hercules.http.HttpServerRequest;
import ru.kontur.vostok.hercules.http.HttpStatusCodes;
import ru.kontur.vostok.hercules.http.handler.HttpHandler;
import ru.kontur.vostok.hercules.protocol.Event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Index multiple documents using default index name if specified.
 * <p>
 * See Elasticsearch Bulk API docs for details.
 *
 * @author Gregory Koshelev
 */
public class BulkHandler implements HttpHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(BulkHandler.class);

    private final IndexManager indexManager;
    private final GateSender gateSender;

    private final EventValidator validator;

    public BulkHandler(IndexManager indexManager, GateSender gateSender) {
        this.indexManager = indexManager;
        this.gateSender = gateSender;

        validator = new EventValidator();
    }

    @Override
    public void handle(HttpServerRequest request) {
        final String defaultIndex = request.getPathParameter("index");
        final String defaultType = request.getPathParameter("type");

        request.readBodyAsync(
                (r, bytes) -> request.dispatchAsync(() -> {
                    try {
                        process(request, bytes, defaultIndex, defaultType);
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

    private void process(HttpServerRequest request, byte[] data, String defaultIndex, String defaultType) {
        Iterator<IndexRequest> iterator = BulkReader.read(data, defaultIndex, defaultType);

        Map<String, List<Event>> events = new HashMap<>();

        while (iterator.hasNext()) {
            IndexRequest indexRequest = iterator.next();

            String index = indexRequest.getAction().getIndex();
            if (index == null) {
                continue;
            }
            IndexMeta meta = indexManager.meta(index);
            if (meta == null) {
                continue;
            }

            Event event = JsonToEventFormatter.format(indexRequest.getDocument(), index, meta);
            if (validator.validate(event)) {//TODO: Errors should be added to response as Elasticsearch do
                events.computeIfAbsent(meta.getStream(), k -> new ArrayList<>(1_000)).add(event);//TODO: Magic number
            }
        }

        for (Map.Entry<String, List<Event>> batch : events.entrySet()) {
            GateStatus status = gateSender.send(batch.getValue(), false, batch.getKey());
            if (status == GateStatus.GATE_UNAVAILABLE) {
                LOGGER.warn("Gate is unavailable: didn't send " + batch.getValue().size() + " events to the stream " + batch.getKey());
                tryComplete(request, HttpStatusCodes.SERVICE_UNAVAILABLE);
            } else if (status == GateStatus.BAD_REQUEST) {
                LOGGER.error("Got bad request from Gate while sending events to the stream " + batch.getKey());
                tryComplete(request, HttpStatusCodes.INTERNAL_SERVER_ERROR);
            }
        }

        tryComplete(request, HttpStatusCodes.OK);//TODO: Make response like Elasticsearch does
    }

    private void tryComplete(HttpServerRequest request, int code) {
        try {
            request.complete(code);
        } catch (Exception ex) {
            LOGGER.error("Error on request completion", ex);
        }
    }
}
