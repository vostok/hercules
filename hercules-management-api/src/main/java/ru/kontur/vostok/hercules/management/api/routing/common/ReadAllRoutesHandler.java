package ru.kontur.vostok.hercules.management.api.routing.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.http.HttpServerRequest;
import ru.kontur.vostok.hercules.http.HttpStatusCodes;
import ru.kontur.vostok.hercules.http.MimeTypes;
import ru.kontur.vostok.hercules.http.handler.HttpHandler;
import ru.kontur.vostok.hercules.routing.config.zk.ZookeeperReadRepository;
import ru.kontur.vostok.hercules.routing.engine.Route;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Handler of HTTP-requests to read all available routes from ZooKeeper.
 *
 * @author Aleksandr Yuferov
 */
public class ReadAllRoutesHandler implements HttpHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReadAllRoutesHandler.class);
    private final ZookeeperReadRepository repository;
    private final ObjectMapper objectMapper;

    public ReadAllRoutesHandler(
            ZookeeperReadRepository repository,
            ObjectMapper objectMapper
    ) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpServerRequest request) {
        try {
            List<Route> routes = repository.fetchAllRoutesFilesNames(null).stream()
                    .map(relativePath -> repository.fetchRouteByRelativePath(relativePath, null))
                    .collect(Collectors.toList());
            String serializedData = objectMapper.writeValueAsString(routes);
            request.complete(HttpStatusCodes.OK, MimeTypes.APPLICATION_JSON, serializedData);
        } catch (Exception exception) {
            LOGGER.error("an error occurred while handling request to read all routes", exception);
            request.complete(HttpStatusCodes.INTERNAL_SERVER_ERROR);
        }
    }
}
