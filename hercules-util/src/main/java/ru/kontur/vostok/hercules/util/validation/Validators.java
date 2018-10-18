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

    public static  <T extends Comparable<T>> Validator<T> greaterThan(T lowerExclusiveBound) {
        return value -> {
            if (lowerExclusiveBound.compareTo(value) < 0) {
                return Optional.empty();
            }
            else {
                return Optional.of(String.format("Value must be greater than '%s' but was '%s'", lowerExclusiveBound, value));
            }
        };
    }

    public static  <T extends Comparable<T>> Validator<T> greaterOrEquals(T lowerInclusiveBound) {
        return value -> {
            if (lowerInclusiveBound.compareTo(value) <= 0) {
                return Optional.empty();
            }
            else {
                return Optional.of(String.format("Value must be greater or equals '%s' but was '%s'", lowerInclusiveBound, value));
            }
        };
    }

    public static  <T extends Comparable<T>> Validator<T> lesserThan(T upperExclusiveBound) {
        return value -> {
            if (0 < upperExclusiveBound.compareTo(value)) {
                return Optional.empty();
            }
            else {
                return Optional.of(String.format("Value must be lesser than '%s' but was '%s'", upperExclusiveBound, value));
            }
        };
    }

    public static  <T extends Comparable<T>> Validator<T> lesserOrEquals(T upperInclusiveBound) {
        return value -> {
            if (0 <= upperInclusiveBound.compareTo(value)) {
                return Optional.empty();
            }
            else {
                return Optional.of(String.format("Value must be lesser or equals '%s' but was '%s'", upperInclusiveBound, value));
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
