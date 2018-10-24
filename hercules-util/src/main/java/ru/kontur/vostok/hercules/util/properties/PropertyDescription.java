package ru.kontur.vostok.hercules.util.properties;

import ru.kontur.vostok.hercules.util.arguments.Preconditions;
import ru.kontur.vostok.hercules.util.functional.Result;
import ru.kontur.vostok.hercules.util.parsing.Parser;
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

    private final String name;
    private final Parser<T> parser;

    private final List<Validator<T>> validators;

    private final boolean required;
    private final T defaultValue;

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
                Optional<String> validationError = validator.validate(defaultValue);
                if (validationError.isPresent()) {
                    throw new IllegalArgumentException(
                            String.format("Validation of default value failed: %s", validationError.get())
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
            }
            else {
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
            Optional<String> validationError = validator.validate(value);
            if (validationError.isPresent()) {
                throw new PropertyException(String.format("Validation of value '%s' of property '%s' failed: %s",
                        stringValue,
                        name,
                        validationError.get()
                ));
            }
        }

        return value;
    }
}
