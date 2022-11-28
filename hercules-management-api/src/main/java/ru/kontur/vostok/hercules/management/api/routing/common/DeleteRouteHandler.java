package ru.kontur.vostok.hercules.management.api.routing.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.http.HttpServerRequest;
import ru.kontur.vostok.hercules.http.HttpStatusCodes;
import ru.kontur.vostok.hercules.http.handler.HttpHandler;
import ru.kontur.vostok.hercules.http.path.PathUtil;
import ru.kontur.vostok.hercules.http.query.QueryUtil;
import ru.kontur.vostok.hercules.routing.config.zk.ZookeeperWriteRepository;
import ru.kontur.vostok.hercules.util.parameter.Parameter;

import java.util.UUID;

/**
 * Handler of HTTP-requests to delete route from ZooKeeper.
 *
 * @author Aleksandr Yuferov
 */
public class DeleteRouteHandler implements HttpHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(DeleteRouteHandler.class);
    private final ZookeeperWriteRepository writeRepository;

    public DeleteRouteHandler(ZookeeperWriteRepository writeRepository) {
        this.writeRepository = writeRepository;
    }

    @Override
    public void handle(HttpServerRequest request) {
        Parameter<UUID>.ParameterValue routeId = PathUtil.get(RoutingQueryParams.REQUIRED_ROUTE_ID, request);
        if (QueryUtil.tryCompleteRequestIfError(request, routeId)) {
            return;
        }
        try {
            if (writeRepository.isNotRouteExists(routeId.get())) {
                request.complete(HttpStatusCodes.NOT_FOUND);
                return;
            }
            if (writeRepository.tryRemoveRouteById(routeId.get())) {
                request.complete(HttpStatusCodes.OK);
                return;
            }
        } catch (Exception exception) {
            LOGGER.error("an error occurred while handle delete route request", exception);
        }
        request.complete(HttpStatusCodes.INTERNAL_SERVER_ERROR);
    }
}
