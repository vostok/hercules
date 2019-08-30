package ru.kontur.vostok.hercules.timeline.api;

import ru.kontur.vostok.hercules.util.parameter.Parameter;
import ru.kontur.vostok.hercules.util.validation.IntegerValidators;
import ru.kontur.vostok.hercules.util.validation.LongValidators;

/**
 * @author Gregory Koshelev
 */
public final class QueryParameters {
    public static final Parameter<String> TIMELINE =
            Parameter.stringParameter("timeline").
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

    public static final Parameter<Long> FROM =
            Parameter.longParameter("from").
                    required().
                    withValidator(LongValidators.positive()).
                    build();

    public static final Parameter<Long> TO =
            Parameter.longParameter("to").
                    required().
                    withValidator(LongValidators.positive()).
                    build();

    private QueryParameters() {
        /* static class */
    }
}
