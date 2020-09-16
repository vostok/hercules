package ru.kontur.vostok.hercules.management.api.timeline;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.curator.exception.CuratorException;
import ru.kontur.vostok.hercules.http.HttpServerRequest;
import ru.kontur.vostok.hercules.http.HttpStatusCodes;
import ru.kontur.vostok.hercules.http.handler.HttpHandler;
import ru.kontur.vostok.hercules.meta.timeline.TimelineRepository;
import ru.kontur.vostok.hercules.undertow.util.HttpResponseContentWriter;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Gregory Koshelev
 */
public class ListTimelineHandler implements HttpHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ListTimelineHandler.class);

    private final TimelineRepository repository;

    public ListTimelineHandler(TimelineRepository repository) {
        this.repository = repository;
    }

    @Override
    public void handle(HttpServerRequest request) {
        List<String> list;
        try {
            list = repository.list().stream()
                    .sorted()
                    .collect(Collectors.toList());
        } catch (CuratorException ex) {
            LOGGER.error("Curator exception when get children", ex);
            request.complete(HttpStatusCodes.INTERNAL_SERVER_ERROR);
            return;
        }

        HttpResponseContentWriter.writeJson(list, request);
    }
}
