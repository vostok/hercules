package ru.kontur.vostok.hercules.util.validation;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Validators - collection of validators
 *
 * @author Kirill Sulim
 */
public final class Validators {

    /**
     * Create validator from predicate {@code predicate} with validation error message {@code errorMessage}
     *
     * @param predicate predicate
     * @param errorMessage validation error message
     * @return validator
     */
    public static <T> Validator<T> fromPredicate(Predicate<T> predicate, String errorMessage) {
        return value -> {
            if (predicate.test(value)) {
                return Optional.empty();
            } else {
                return Optional.of(errorMessage);
            }
        };
    }

    /**
     * Create greater than {@code lowerExclusiveBound} validator
     *
     * @param lowerExclusiveBound lower exclusive bound
     * @return validator
     */
    public static <T extends Comparable<T>> Validator<T> greaterThan(T lowerExclusiveBound) {
        return value -> {
            if (lowerExclusiveBound.compareTo(value) < 0) {
                return Optional.empty();
            } else {
                return Optional.of(String.format("Value must be greater than '%s' but was '%s'", lowerExclusiveBound, value));
            }
        };
    }

    /**
     * Create greater or equals {@code lowerInclusiveBound} validator
     *
     * @param lowerInclusiveBound lower inclusive bound
     * @return validator
     */
    public static  <T extends Comparable<T>> Validator<T> greaterOrEquals(T lowerInclusiveBound) {
        return value -> {
            if (lowerInclusiveBound.compareTo(value) <= 0) {
                return Optional.empty();
            } else {
                return Optional.of(String.format("Value must be greater or equals '%s' but was '%s'", lowerInclusiveBound, value));
            }
        };
    }

    /**
     * Create lesser than {@code upperExclusiveBound} validator
     *
     * @param upperExclusiveBound upper exclusive bound
     * @return validator
     */
    public static  <T extends Comparable<T>> Validator<T> lesserThan(T upperExclusiveBound) {
        return value -> {
            if (0 < upperExclusiveBound.compareTo(value)) {
                return Optional.empty();
            } else {
                return Optional.of(String.format("Value must be lesser than '%s' but was '%s'", upperExclusiveBound, value));
            }
        };
    }

    /**
     * Create lesser or equals {@code upperInclusiveBound} validator
     *
     * @param upperInclusiveBound upper inclusive bound
     * @return validator
     */
    public static  <T extends Comparable<T>> Validator<T> lesserOrEquals(T upperInclusiveBound) {
        return value -> {
            if (0 <= upperInclusiveBound.compareTo(value)) {
                return Optional.empty();
            } else {
                return Optional.of(String.format("Value must be lesser or equals '%s' but was '%s'", upperInclusiveBound, value));
            }
        };
    }

    /**
     * Create interval between {@code lowerInclusiveBound} inclusive and {@code upperExclusiveBound} exclusive validator
     *
     * @param lowerInclusiveBound lower inclusive bound
     * @param upperExclusiveBound upper exclusive bound
     * @return validator
     */
    public static <T extends Comparable<T>> Validator<T> interval(T lowerInclusiveBound, T upperExclusiveBound) {
        return value -> {
            if (lowerInclusiveBound.compareTo(value) <= 0 && 0 < upperExclusiveBound.compareTo(value)) {
                return Optional.empty();
            } else {
                return Optional.of(String.format(
                        "Value must be between '%s' (inclusive) and '%s' (exclusive), but was '%s'",
                        lowerInclusiveBound,
                        upperExclusiveBound,
                        value
                ));
            }
        };
    }

    /**
     * Return port validator
     *
     * @return validator
     */
    public static Validator<Integer> portValidator() {
        return interval(0, 65_536);
    }

    private Validators() {
        /* static class */
    }
}
