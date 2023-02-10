package ru.kontur.vostok.hercules.sink;

import ru.kontur.vostok.hercules.util.parameter.Parameter;

/**
 * @author Innokentiy Krivonosov
 */
public class SinkProps {
    public static final Parameter<String[]> PATTERN =
            Parameter.stringArrayParameter("pattern")
                    .required()
                    .build();

    public static final Parameter<String[]> PATTERN_EXCLUSIONS =
            Parameter.stringArrayParameter("pattern.exclusions")
                    .withDefault(new String[0])
                    .build();

    public static final Parameter<String> GROUP_ID =
            Parameter.stringParameter("groupId")
                    .build();

    public static final Parameter<Integer> BATCH_SIZE =
            Parameter.integerParameter("batchSize")
                    .withDefault(1000)
                    .build();

    public static final Parameter<Long> POLL_TIMEOUT_MS =
            Parameter.longParameter("pollTimeoutMs")
                    .withDefault(6_000L)
                    .build();

    public static final Parameter<Long> AVAILABILITY_TIMEOUT_MS =
            Parameter.longParameter("availabilityTimeoutMs")
                    .withDefault(2_000L)
                    .build();
}
