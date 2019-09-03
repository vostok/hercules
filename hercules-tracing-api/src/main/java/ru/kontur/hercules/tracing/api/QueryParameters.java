package ru.kontur.hercules.tracing.api;

import ru.kontur.vostok.hercules.util.parameter.Parameter;
import ru.kontur.vostok.hercules.util.parameter.parsing.Parsers;
import ru.kontur.vostok.hercules.util.validation.IntegerValidators;

import java.util.UUID;

/**
 * @author Gregory Koshelev
 */
public final class QueryParameters {
    public static final Parameter<UUID> TRACE_ID =
            Parameter.parameter("traceId", Parsers.fromFunction(UUID::fromString)).
                    required().
                    build();

    public static final Parameter<UUID> PARENT_SPAN_ID =
            Parameter.parameter("parentSpanId", Parsers.fromFunction(UUID::fromString)).
                    build();

    public static final Parameter<Integer> LIMIT =
            Parameter.integerParameter("limit").
                    withDefault(10_000).
                    withValidator(IntegerValidators.positive()).
                    build();

    public static final Parameter<String> PAGING_STATE =
            Parameter.stringParameter("pagingState").
                    build();

    private QueryParameters() {
        /* static class */
    }
}
