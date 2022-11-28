package ru.kontur.vostok.hercules.management.api.routing.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.http.HttpServerRequest;
import ru.kontur.vostok.hercules.http.HttpStatusCodes;
import ru.kontur.vostok.hercules.http.MimeTypes;
import ru.kontur.vostok.hercules.http.handler.HttpHandler;
import ru.kontur.vostok.hercules.http.path.PathUtil;
import ru.kontur.vostok.hercules.http.query.QueryUtil;
import ru.kontur.vostok.hercules.routing.config.zk.ZookeeperReadRepository;
import ru.kontur.vostok.hercules.routing.engine.Route;
import ru.kontur.vostok.hercules.util.parameter.Parameter;

import java.util.UUID;

/**
 * Handler of HTTP-requests to read route by its id from ZooKeeper.
 *
 * @author Aleksandr Yuferov
 */
public class ReadRouteByIdHandler implements HttpHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReadRouteByIdHandler.class);
    private final ZookeeperReadRepository repository;
    private final ObjectMapper objectMapper;

    public ReadRouteByIdHandler(
            ZookeeperReadRepository repository,
            ObjectMapper objectMapper
    ) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpServerRequest request) {
        Parameter<UUID>.ParameterValue routeId = PathUtil.get(RoutingQueryParams.REQUIRED_ROUTE_ID, request);
        if (QueryUtil.tryCompleteRequestIfError(request, routeId)) {
            return;
        }
        try {
            Route route = repository.fetchRouteById(routeId.get(), null);
            if (route == null) {
                request.complete(HttpStatusCodes.NOT_FOUND);
                return;
            }
            String serializedData = objectMapper.writeValueAsString(route);
            request.complete(HttpStatusCodes.OK, MimeTypes.APPLICATION_JSON, serializedData);
        } catch (Exception exception) {
            LOGGER.error("an error occurred while handle read route by id request");
            request.complete(HttpStatusCodes.INTERNAL_SERVER_ERROR);
        }
    }
}
