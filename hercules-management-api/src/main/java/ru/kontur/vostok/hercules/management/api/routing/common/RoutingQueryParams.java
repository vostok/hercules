package ru.kontur.vostok.hercules.management.api.routing.common;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.kontur.vostok.hercules.util.parameter.Parameter;
import ru.kontur.vostok.hercules.util.parameter.parsing.ParsingResult;

import java.util.UUID;

/**
 * @author Aleksandr Yuferov
 */
public class RoutingQueryParams {
    public static final Parameter<UUID> REQUIRED_ROUTE_ID = Parameter
            .parameter("routeId", RoutingQueryParams::parseUuid)
            .required()
            .build();

    public static final Parameter<UUID> OPTIONAL_ROUTE_ID = Parameter
            .parameter("routeId", RoutingQueryParams::parseUuid)
            .build();

    @NotNull
    private static ParsingResult<UUID> parseUuid(@Nullable String value) {
        if (value == null) {
            return ParsingResult.empty();
        }
        try {
            return ParsingResult.of(UUID.fromString(value));
        } catch (Exception exception) {
            return ParsingResult.invalid("invalid UUID string");
        }
    }
}
