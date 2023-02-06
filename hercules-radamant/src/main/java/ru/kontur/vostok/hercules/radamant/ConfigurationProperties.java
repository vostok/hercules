package ru.kontur.vostok.hercules.radamant;

import ru.kontur.vostok.hercules.util.parameter.Parameter;
import ru.kontur.vostok.hercules.util.validation.ValidationResult;

/**
 * @author Tatyana Tokmyanina
 */
public class ConfigurationProperties {
    public static final Parameter<String[]> PATTERN = Parameter
            .stringArrayParameter("radamant.pattern")
            .required()
            .build();

    public static final Parameter<String> DEFAULT_RESULT_STREAM = Parameter
            .stringParameter("radamant.default.result.stream")
            .required()
            .build();

    public static final Parameter<String> CONFIG_PATH =
            Parameter.stringParameter("radamant.config.path")
                    .withDefault("file://rules_flat.json")
                    .withValidator(v -> {
                        final String prefix = "file://";
                        if (v.startsWith(prefix)) {
                            return ValidationResult.ok();
                        }
                        return ValidationResult.error("Value should start with " + prefix);
                    })
                    .build();
}
