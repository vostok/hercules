package ru.kontur.vostok.hercules.util.validation;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Validators
 *
 * @author Kirill Sulim
 */
public final class Validators {

    public static <T> Validator<T> fromPredicate(Predicate<T> predicate, String errorMessage) {
        return value -> {
            if (predicate.test(value)) {
                return Optional.empty();
            }
            else {
                return Optional.of(errorMessage);
            }
        };
    }

    public static  <T extends Comparable<T>> Validator<T> greaterThan(T lowerExclusiveBond) {
        return value -> {
            if (lowerExclusiveBond.compareTo(value) < 0) {
                return Optional.empty();
            }
            else {
                return Optional.of(String.format("Value must be greater than '%s' but was '%s'", lowerExclusiveBond, value));
            }
        };
    }

    public static  <T extends Comparable<T>> Validator<T> greaterOrEquals(T lowerInclusiveBond) {
        return value -> {
            if (lowerInclusiveBond.compareTo(value) <= 0) {
                return Optional.empty();
            }
            else {
                return Optional.of(String.format("Value must be greater or equals '%s' but was '%s'", lowerInclusiveBond, value));
            }
        };
    }

    public static  <T extends Comparable<T>> Validator<T> lesserThan(T upperExclusiveBond) {
        return value -> {
            if (0 < upperExclusiveBond.compareTo(value)) {
                return Optional.empty();
            }
            else {
                return Optional.of(String.format("Value must be lesser than '%s' but was '%s'", upperExclusiveBond, value));
            }
        };
    }

    public static  <T extends Comparable<T>> Validator<T> lesserOrEquals(T upperInclusiveBond) {
        return value -> {
            if (0 <= upperInclusiveBond.compareTo(value)) {
                return Optional.empty();
            }
            else {
                return Optional.of(String.format("Value must be lesser or equals '%s' but was '%s'", upperInclusiveBond, value));
            }
        };
    }

    public static Validator<String> notEmpty() {
        return value -> {
            if (Objects.nonNull(value) && !value.isEmpty()) {
                return Optional.empty();
            }
            else {
                return Optional.of("String is empty");
            }
        };
    }

    private Validators() {
        /* static class */
    }
}
