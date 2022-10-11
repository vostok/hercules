package ru.kontur.vostok.hercules.splitter;

import org.jetbrains.annotations.PropertyKey;
import ru.kontur.vostok.hercules.util.parameter.Parameter;

/**
 * Specific configuration properties descriptors of splitter application.
 *
 * @author Aleksandr Yuferov
 */
public class ConfigurationProperties {
    private static final String BUNDLE = "application";

    @PropertyKey(resourceBundle = BUNDLE)
    public static final Parameter<String[]> SHARDING_KEYS = Parameter.stringArrayParameter("splitter.shardingKeys")
            .required()
            .build();

    @PropertyKey(resourceBundle = BUNDLE)
    public static final Parameter<String> HASH_ALGORITHM = Parameter.stringParameter("splitter.hashAlgorithm")
            .required()
            .build();

    @PropertyKey(resourceBundle = BUNDLE)
    public static final Parameter<String[]> PATTERN = Parameter.stringArrayParameter("splitter.pattern")
            .required()
            .build();

    @PropertyKey(resourceBundle = BUNDLE)
    public static final Parameter<String[]> PATTERN_EXCLUSIONS = Parameter.stringArrayParameter("splitter.pattern.exclusions")
            .withDefault(new String[0])
            .build();

    @PropertyKey(resourceBundle = BUNDLE)
    public static final Parameter<String> OUTPUT_STREAM_TEMPLATE = Parameter.stringParameter("splitter.outputStreamTemplate")
            .required()
            .build();

    public static final String PARTITION_WEIGHTS_SCOPE = "splitter.nodeWeights";

    private ConfigurationProperties() {
    }
}
