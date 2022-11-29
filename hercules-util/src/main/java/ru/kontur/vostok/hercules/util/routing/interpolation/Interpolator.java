package ru.kontur.vostok.hercules.util.routing.interpolation;

import com.google.common.base.Preconditions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * A class that performs string interpolation.
 * <p>
 * String interpolation is a special syntax to include evaluable expressions into strings.
 * <p>
 * Special templates that should be replaced with a value in a process of interpolation are called interpolation expressions and represented in a code as
 * {@link InterpolationExpression}. The syntax of interpolation expressions is follows template: {@code {<namespace>:<variable>}}. The namespace and variable
 * uniquely identify the value to be substituted for the interpolation expression.
 * <p>
 * String that contains or potentially contains interpolation expressions are called templated strings or simply template.
 * <p>
 * The nested interpolations are prohibited. Templated string can contain multiple interpolation expressions.
 *
 * @author Aleksandr Yuferov
 */
public class Interpolator {

    /**
     * Create a new interpolation context.
     *
     * @return Created context.
     */
    public Context createContext() {
        return new Context();
    }

    /**
     * Extract interpolations from templated string.
     * <p/>
     * I.e. for templated string {@code "{foo:bar}_abc123{foo:baz}"} method will return a list containing two descriptors:
     * {@code { namespace = foo, variable = bar }} and {@code { namespace = foo, variable = baz }}.
     * <p/>
     * Duplicates will not be removed.
     *
     * @param templatedString Templated string.
     * @return List of extracted interpolations.
     */
    public List<InterpolationExpression> extractInterpolations(String templatedString) {
        List<InterpolationExpression> result = new ArrayList<>();
        parse(templatedString, result::add, ch -> {
        });
        return result;
    }

    /**
     * Do interpolation.
     * <p/>
     * This method replaces all interpolations in templated string using given context.
     *
     * @param templatedString Templated string.
     * @param context         Interpolation context.
     * @return String with all replaced interpolations.
     */
    public String interpolate(String templatedString, Context context) {
        try {
            StringBuilder result = new StringBuilder(templatedString.length());
            parse(templatedString, interpolation -> {
                CharSequence replacement = context.stringValueOf(interpolation);
                Preconditions.checkState(replacement != null);
                result.append(replacement);
            }, result::append);
            return result.toString();
        } catch (IllegalStateException ignore) {
            return null;
        }
    }

    private static void parse(
            String templatedString,
            Consumer<InterpolationExpression> interpolationConsumer,
            CharConsumer regularSymbolsConsumer
    ) {
        int length = templatedString.length();
        for (int i = 0; i < length; i++) {
            char currentChar = templatedString.charAt(i);
            if (currentChar == '{') {
                int end = findEnclosingChar(templatedString, i);
                var interpolation = InterpolationExpression.fromString(templatedString.substring(i + 1, end));
                interpolationConsumer.accept(interpolation);
                i = end;
            } else {
                regularSymbolsConsumer.accept(currentChar);
            }
        }
    }

    private static int findEnclosingChar(String template, int from) {
        int end = from + 1;
        int length = template.length();
        for (; end < length; end++) {
            char endChar = template.charAt(end);
            if (endChar == '{') {
                throw new IncorrectInterpolationSyntaxException("nested interpolations are prohibited");
            } else if (endChar == '}') {
                break;
            }
        }
        if (end == length) {
            throw new IncorrectInterpolationSyntaxException("interpolation not closed");
        }
        return end;
    }

    /**
     * Interpolation context.
     * <p>
     * A container of values for interpolation expressions that will be replaced in templated string when
     * {@link Interpolator#interpolate(String, Context)} method will be called.
     */
    public static class Context {

        private final Map<InterpolationExpression, CharSequence> data = new HashMap<>();

        /**
         * Adds value of interpolation expression to context.
         * <p/>
         * Shortcut for a method call {@link #add(InterpolationExpression, CharSequence)}.
         *
         * @param namespace Namespace of interpolation expression.
         * @param variable  Variable of interpolation expression.
         * @param value     Value of the given interpolation expression.
         * @return This context reference.
         * @see #add(InterpolationExpression, CharSequence)
         */
        public Context add(String namespace, String variable, CharSequence value) {
            return add(InterpolationExpression.of(namespace, variable), value);
        }

        /**
         * Adds value of interpolation expression to context.
         * <p/>
         * Puts data into context.
         *
         * @param interpolationExpr Interpolation expression that should be replaced by value.
         * @param value             Value of the given interpolation expression.
         * @return This context reference.
         */
        public Context add(InterpolationExpression interpolationExpr, CharSequence value) {
            data.put(interpolationExpr, value);
            return this;
        }

        /**
         * Get value of interpolation expression from context.
         *
         * @param interpolationExpr Interpolation expression.
         * @return The replacement of given interpolation descriptor contained in context or null if there is no associated value.
         */
        public CharSequence stringValueOf(InterpolationExpression interpolationExpr) {
            return data.get(interpolationExpr);
        }
    }

    @FunctionalInterface
    private interface CharConsumer {

        void accept(char ch);
    }
}
