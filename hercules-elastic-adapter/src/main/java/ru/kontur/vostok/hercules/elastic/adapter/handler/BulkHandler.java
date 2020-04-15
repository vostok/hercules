package ru.kontur.vostok.hercules.elastic.adapter.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.elastic.adapter.bulk.BulkReader;
import ru.kontur.vostok.hercules.elastic.adapter.bulk.IndexRequest;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
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
        List<Event> leproseryEvents = new ArrayList<>(100);//TODO: Magic number

        while (iterator.hasNext()) {
            IndexRequest indexRequest = iterator.next();

            String index = indexRequest.getAction().getIndex();
            if (index == null) {
                //TODO: Leprosery
                continue;
            }
            IndexMeta meta = indexManager.meta(index);
            if (meta == null) {
                //TODO: Leprosery
                continue;
            }

            Result<Event, String> result = LogEventMapper.from(indexRequest.getDocument(), meta.getProperties(), index);
            if (result.isOk() && validator.validate(result.get())) {
                events.computeIfAbsent(meta.getStream(), k -> new ArrayList<>(1_000)).add(result.get());//TODO: Magic number
            } else {
                //TODO: Leprosery
            }
        }

        for (Map.Entry<String, List<Event>> batch : events.entrySet()) {
            GateStatus status = gateSender.send(batch.getValue(), false, batch.getKey());
            if (status != GateStatus.OK) {
                //TODO: should retry the request or send 503 back
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
