package ru.kontur.vostok.hercules.management.api;

import ru.kontur.vostok.hercules.util.parameter.Parameter;
import ru.kontur.vostok.hercules.util.validation.LongValidators;
import ru.kontur.vostok.hercules.util.validation.StringValidators;
import ru.kontur.vostok.hercules.util.validation.ValidationResult;

/**
 * @author Gregory Koshelev
 */
public final class QueryParameters {
    public static Parameter<String> ASYNC =
            Parameter.stringParameter("async").build();

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
                    withValidator(LongValidators.positive()).
                    build();

    public static Parameter<Integer> NEW_PARTITIONS =
            Parameter.integerParameter("newPartitions").
                    required().
                    withValidator(
                            value ->
                                    (value > 0 && value <= 48)
                                            ? ValidationResult.ok()
                                            : ValidationResult.error("Value should be >0 and <=48")
                    ).
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
