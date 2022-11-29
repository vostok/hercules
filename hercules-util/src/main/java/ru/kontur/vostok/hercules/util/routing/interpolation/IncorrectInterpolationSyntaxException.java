package ru.kontur.vostok.hercules.util.routing.interpolation;

/**
 * Incorrect interpolation syntax.
 * <p>
 * Exception will be thrown by {@link Interpolator} and {@link InterpolationExpression} classes if syntax errors will be found
 * in templated strings.
 *
 * @author Aleksandr Yuferov
 */
public class IncorrectInterpolationSyntaxException extends IllegalArgumentException {
    public IncorrectInterpolationSyntaxException(String s) {
        super(s);
    }
}
