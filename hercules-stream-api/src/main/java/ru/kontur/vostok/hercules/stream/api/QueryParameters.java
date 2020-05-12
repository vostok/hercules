package ru.kontur.vostok.hercules.stream.api;

import ru.kontur.vostok.hercules.util.parameter.Parameter;
import ru.kontur.vostok.hercules.util.validation.IntegerValidators;

/**
 * @author Gregory Koshelev
 */
public class QueryParameters {
    public static final Parameter<String> STREAM =
            Parameter.stringParameter("stream").
                    required().
                    build();

    public static final Parameter<Integer> SHARD_INDEX =
            Parameter.integerParameter("shardIndex").
                    required().
                    withValidator(IntegerValidators.nonNegative()).
                    build();

    public static final Parameter<Integer> SHARD_COUNT =
            Parameter.integerParameter("shardCount").
                    required().
                    withValidator(IntegerValidators.positive()).
                    build();

    public static final Parameter<Integer> TAKE =
            Parameter.integerParameter("take").
                    required().
                    withValidator(IntegerValidators.positive()).
                    build();

    public static final Parameter<Integer> TIMEOUT_MS =
            Parameter.integerParameter("timeoutMs").
                    withDefault(1_000).
                    withValidator(IntegerValidators.range(500, 10_000)).
                    build();
}
