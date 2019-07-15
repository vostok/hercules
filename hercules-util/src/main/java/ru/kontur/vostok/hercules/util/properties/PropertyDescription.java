package ru.kontur.vostok.hercules.util.properties;

import ru.kontur.vostok.hercules.util.arguments.Preconditions;
import ru.kontur.vostok.hercules.util.functional.Result;
import ru.kontur.vostok.hercules.util.parsing.Parser;
import ru.kontur.vostok.hercules.util.validation.ValidationResult;
import ru.kontur.vostok.hercules.util.validation.Validator;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;

/**
 * PropertyDescription - describe rules of how to extract and check property value
 *
 * @author Kirill Sulim
 */
public class PropertyDescription<T> {

    /**
     * Property name
     */
    private final String name;

    /**
     * Property parser
     */
    private final Parser<T> parser;

    /**
     * List of validator of parsed value
     */
    private final List<Validator<T>> validators;

    /**
     * Required flag
     */
    private final boolean required;

    /**
     * Default property value
     */
    private final T defaultValue;

    /**
     * Create property description
     *
     * @param name property name
     * @param parser value parser
     * @param validators list of value validators
     * @param required required flag
     * @param defaultValue default value
     */
    public PropertyDescription(String name, Parser<T> parser, List<Validator<T>> validators, boolean required, T defaultValue) {
        this.name = name;
        this.parser = parser;
        this.validators = validators;
        this.required = required;
        this.defaultValue = defaultValue;

        Preconditions.checkNotNull(name);
        Preconditions.checkNotNull(parser);

        if (!required) {
            for (Validator<T> validator : validators) {
                ValidationResult result = validator.validate(defaultValue);
                if (result.isError()) {
                    throw new IllegalArgumentException(
                            String.format("Validation of default value failed: %s", result.error())
                    );
                }
            }
        }
    }

    /**
     * Extracts described property from {@code properties} and make all required checks
     *
     * @param properties from where property will be extracted
     * @return property value
     * @throws PropertyException in case some checks failed
     */
    public T extract(Properties properties) {
        String stringValue = properties.getProperty(name);
        if (Objects.isNull(stringValue)) {
            if (required) {
                throw new PropertyException(String.format("Missing required property '%s'", name));
            } else {
                return defaultValue;
            }
        }

        Result<T, String> parseResult = parser.parse(stringValue);
        if (!parseResult.isOk()) {
            throw new PropertyException(String.format("Cannot parse value '%s' of property '%s': %s",
                    stringValue,
                    name,
                    parseResult.getError()
            ));
        }

        T value = parseResult.get();
        for (Validator<T> validator : validators) {
            ValidationResult result = validator.validate(value);
            if (result.isError()) {
                throw new PropertyException(String.format("Validation of value '%s' of property '%s' failed: %s",
                        stringValue,
                        name,
                        result.error()
                ));
            }
        }

        return value;
    }
}
