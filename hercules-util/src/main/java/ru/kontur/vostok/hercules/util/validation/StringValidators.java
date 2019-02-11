package ru.kontur.vostok.hercules.util.validation;

import java.util.Objects;
import java.util.Optional;

/**
 * @author Petr Demenev
 */
public class StringValidators {

    /**
     * Return non empty string validator
     *
     * @return validator
     */
    public static Validator<String> notEmpty() {
        return value -> {
            if (Objects.nonNull(value) && !value.isEmpty()) {
                return Optional.empty();
            } else {
                return Optional.of("String is empty");
            }
        };
    }

    /**
     * Return matching string validator
     *
     * @return validator
     */
    public static Validator<String> matchesWith(String regex) {
        return value -> {
            if (value.matches(regex)) {
                return Optional.empty();
            } else {
                return Optional.of(String.format("String should match the pattern but was '%s'", value));
            }
        };
    }

    public StringValidators() {
    }
}
