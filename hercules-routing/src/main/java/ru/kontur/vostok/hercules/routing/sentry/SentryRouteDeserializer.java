package ru.kontur.vostok.hercules.routing.sentry;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import ru.kontur.vostok.hercules.routing.engine.RouteDeserializer;
import ru.kontur.vostok.hercules.routing.engine.tree.DecisionTreeEngineRoute;
import ru.kontur.vostok.hercules.util.routing.SentryDestination;

import java.io.IOException;
import java.util.UUID;

/**
 * Deserializer for routes intended for routing
 *
 * @author Aleksandr Yuferov
 */
public class SentryRouteDeserializer implements RouteDeserializer {
    private static final TypeReference<DecisionTreeEngineRoute.Builder<SentryDestination>> BUILDER_TYPE =
            new TypeReference<>() {
            };
    private final ObjectMapper objectMapper;

    public SentryRouteDeserializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public DecisionTreeEngineRoute<SentryDestination> deserialize(UUID id, byte[] data) throws IOException {
        return objectMapper.readValue(data, BUILDER_TYPE)
                .setId(id)
                .build();
    }
}
