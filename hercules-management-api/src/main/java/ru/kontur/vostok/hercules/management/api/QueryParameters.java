package ru.kontur.vostok.hercules.management.api;

import ru.kontur.vostok.hercules.meta.stream.validation.StreamValidators;
import ru.kontur.vostok.hercules.util.parameter.Parameter;
import ru.kontur.vostok.hercules.util.validation.IntegerValidators;
import ru.kontur.vostok.hercules.util.validation.StringValidators;

/**
 * @author Gregory Koshelev
 */
public final class QueryParameters {
    public static Parameter<String> ASYNC =
            Parameter.stringParameter("async").build();

    public static Parameter<Integer> TIMEOUT_MS =
            Parameter.integerParameter("timeoutMs").
                    withDefault(15_000).
                    withValidator(IntegerValidators.rangeInclusive(1_000, 30_000)).
                    build();

    public static Parameter<String> STREAM =
            Parameter.stringParameter("stream").
                    required().
                    build();

    public static Parameter<String> TIMELINE =
            Parameter.stringParameter("timeline").
                    required().
                    build();

    public static Parameter<Long> NEW_TTL =
            Parameter.longParameter("newTtl").
                    required().
                    withValidator(StreamValidators.TTL_VALIDATOR).
                    build();

    public static Parameter<String> NEW_DESCRIPTION =
            Parameter.stringParameter("newDescription").
                    required().
                    withValidator(StreamValidators.DESCRIPTION_VALIDATOR).
                    build();

    public static Parameter<Integer> NEW_PARTITIONS =
            Parameter.integerParameter("newPartitions").
                    required().
                    withValidator(StreamValidators.PARTITION_VALIDATOR).
                    build();

    public static Parameter<String> KEY =
            Parameter.stringParameter("key").
                    required().
                    withValidator(StringValidators.matchesWith("^[a-z0-9_]*[a-f0-9]{32}$")).
                    build();

    public static Parameter<String> PATTERN =
            Parameter.stringParameter("pattern").
                    required().//TODO: Add validation on pattern format
                    build();

    public static Parameter<String> RIGHTS =
            Parameter.stringParameter("rights").
                    required().//TODO: Add validation on rights format
                    build();

    private QueryParameters() {
        /* static class */
    }
}
