package ru.kontur.vostok.hercules.util.validation;

import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Petr Demenev
 */
public final class StringValidators {

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
        final Pattern pattern = Pattern.compile(regex);
        return value -> {
            Matcher matcher = pattern.matcher(value);
            if (matcher.matches()) {
                return Optional.empty();
            } else {
                return Optional.of(String.format("String should match the pattern '%s' but was '%s'",
                        regex, value));
            }
        };
    }

    private StringValidators() {
        /* static class */
    }
}
