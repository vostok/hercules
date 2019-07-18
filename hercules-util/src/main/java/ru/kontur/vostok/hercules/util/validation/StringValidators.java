package ru.kontur.vostok.hercules.util.validation;

import ru.kontur.vostok.hercules.util.text.StringUtil;

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
        return value ->
                (!StringUtil.isNullOrEmpty(value))
                        ? ValidationResult.ok()
                        : ValidationResult.error("String is empty");
    }

    /**
     * Return matching string validator
     *
     * @return validator
     */
    public static Validator<String> matchesWith(String regex) {
        final Pattern pattern = Pattern.compile(regex);
        return value ->
                (pattern.matcher(value).matches())
                        ? ValidationResult.ok()
                        : ValidationResult.error("String should match the pattern '" + regex + "' but was '" + value + "'");
    }

    private StringValidators() {
        /* static class */
    }
}
