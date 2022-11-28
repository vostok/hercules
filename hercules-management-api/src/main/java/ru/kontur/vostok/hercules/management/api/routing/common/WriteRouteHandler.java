package ru.kontur.vostok.hercules.management.api.routing.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.http.HttpServerRequest;
import ru.kontur.vostok.hercules.http.HttpStatusCodes;
import ru.kontur.vostok.hercules.http.handler.HttpHandler;
import ru.kontur.vostok.hercules.http.path.PathUtil;
import ru.kontur.vostok.hercules.routing.config.zk.ZookeeperWriteRepository;
import ru.kontur.vostok.hercules.routing.engine.Route;
import ru.kontur.vostok.hercules.routing.engine.RouteDeserializer;
import ru.kontur.vostok.hercules.util.parameter.Parameter;
import ru.kontur.vostok.hercules.util.validation.ValidationResult;
import ru.kontur.vostok.hercules.util.validation.Validator;

import java.util.UUID;

/**
 * Handler of HTTP-requests to create/change routes in ZooKeeper.
 *
 * @author Aleksandr Yuferov
 */
public class WriteRouteHandler implements HttpHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(WriteRouteHandler.class);
    private final ZookeeperWriteRepository repository;
    private final RouteDeserializer deserializer;
    private final Validator<Route> validator;

    public WriteRouteHandler(
            ZookeeperWriteRepository repository,
            RouteDeserializer deserializer,
            Validator<Route> validator
    ) {
        this.repository = repository;
        this.deserializer = deserializer;
        this.validator = validator;
    }

    @Override
    public void handle(HttpServerRequest request) {
        request.readBodyAsync(this::handleData);
    }

    private void handleData(HttpServerRequest request, byte[] data) {
        Parameter<UUID>.ParameterValue routeId = PathUtil.get(RoutingQueryParams.OPTIONAL_ROUTE_ID, request);
        try {
            Route route = deserializer.deserialize(routeId.orEmpty(null), data);
            ValidationResult validationResult = validator.validate(route);
            if (validationResult.isError()) {
                request.complete(HttpStatusCodes.BAD_REQUEST, "plain/text", validationResult.error());
                return;
            }
            if (routeId.isEmpty()) {
                UUID createdRouteId = repository.tryCreateRoute(route);
                if (createdRouteId != null) {
                    request.complete(HttpStatusCodes.OK, "application/json",
                            createRouteResponseBody(createdRouteId));
                    return;
                }
            } else {
                if (repository.isNotRouteExists(routeId.get())) {
                    request.complete(HttpStatusCodes.NOT_FOUND);
                    return;
                }
                if (repository.tryUpdateRoute(route)) {
                    request.complete(HttpStatusCodes.OK);
                    return;
                }
            }
        } catch (Exception exception) {
            LOGGER.error("an error occurred while handle write route request", exception);
        }
        request.complete(HttpStatusCodes.INTERNAL_SERVER_ERROR);
    }

    private String createRouteResponseBody(UUID routeId) {
        return String.format("{ \"routeId\": \"%s\" }", routeId);
    }
}
