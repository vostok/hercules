package ru.kontur.vostok.hercules.util.routing.interpolation;

import java.util.Objects;

/**
 * Interpolation expression.
 * <p/>
 * This class represents the interpolation expression from templated string.
 *
 * @author Aleksandr Yuferov
 * @see Interpolator
 */
public class InterpolationExpression {
    private final String namespace;
    private final String variable;

    /**
     * Create interpolation object from the string.
     * <p>
     * Parse strings that are follows template: {@code <namespace>:<variable> }.
     * If value have incorrect syntax then {@link IncorrectInterpolationSyntaxException} will be thrown.
     *
     * @param value String for parsing.
     * @return Created object.
     */
    public static InterpolationExpression fromString(String value) {
        int nsDelimiterIndex = value.indexOf(':');
        if (nsDelimiterIndex == -1) {
            throw new IncorrectInterpolationSyntaxException("namespace delimiter not found");
        }
        String namespace = value.substring(0, nsDelimiterIndex);
        if (namespace.isBlank()) {
            throw new IncorrectInterpolationSyntaxException("namespace name not found");
        }
        String variable = value.substring(nsDelimiterIndex + 1);
        if (variable.isBlank()) {
            throw new IncorrectInterpolationSyntaxException("variable name not found");
        }
        return new InterpolationExpression(namespace, variable);
    }

    public static InterpolationExpression of(String namespace, String value) {
        return new InterpolationExpression(namespace, value);
    }

    protected InterpolationExpression(String namespace, String variable) {
        this.namespace = Objects.requireNonNull(namespace, "namespace");
        this.variable = Objects.requireNonNull(variable, "variable");
    }

    public String namespace() {
        return namespace;
    }

    public String variable() {
        return variable;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        InterpolationExpression that = (InterpolationExpression) other;
        return namespace.equals(that.namespace) && variable.equals(that.variable);
    }

    @Override
    public int hashCode() {
        return Objects.hash(namespace, variable);
    }
}
