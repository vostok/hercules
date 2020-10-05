package ru.kontur.vostok.hercules.util.parameter;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.kontur.vostok.hercules.util.parameter.parsing.Parser;
import ru.kontur.vostok.hercules.util.parameter.parsing.Parsers;
import ru.kontur.vostok.hercules.util.parameter.parsing.ParsingResult;
import ru.kontur.vostok.hercules.util.validation.ValidationResult;
import ru.kontur.vostok.hercules.util.validation.Validator;
import ru.kontur.vostok.hercules.util.validation.Validators;


/**
 * Parameter
 *
 * @param <T> the type of parameter value
 * @author Gregory Koshelev
 */
public class Parameter<T> {
    private final ParameterValue empty = new ParameterValue(null, ValidationResult.ok());
    private final ParameterValue missed = new ParameterValue(null, ValidationResult.missed());

    private final String name;
    private final ParameterType type;
    private final ParameterValue defaultValue;
    private final Parser<T> parser;
    private final Validator<T> validator;

    private Parameter(String name, ParameterType type, T defaultValue, @NotNull Parser<T> parser, Validator<T> validator) {
        this.name = name;
        this.type = type;
        this.defaultValue = defaultValue != null ? of(defaultValue) : empty();
        this.parser = parser;
        this.validator = validator;
    }

    /**
     * Extracts parameter's value from the string.
     *
     * @param string the string
     * @return extracted value
     */
    public ParameterValue from(String string) {
        ParsingResult<T> parsed = parser.parse(string);

        if (parsed.hasError()) {
            return invalid(parsed.error());
        }

        if (parsed.isEmpty()) {
            if (type == ParameterType.OPTIONAL) {
                return empty();
            }

            if (type == ParameterType.REQUIRED) {
                return missed();
            }

            return defaultValue;
        }

        T value = parsed.get();
        ValidationResult result = validator.validate(value);
        if (result.isOk()) {
            return of(value);
        }
        return invalid(result);
    }

    /**
     * Parameter's name.
     *
     * @return the name
     */
    public String name() {
        return name;
    }

    /**
     * Builds valid parameter's value.
     *
     * @param value the value
     * @return valid parameter's value
     */
    @NotNull
    private ParameterValue of(@NotNull T value) {
        return new ParameterValue(value, ValidationResult.ok());
    }

    /**
     * Builds invalid parameter's value.
     *
     * @param result the validation result
     * @return invalid parameter's value
     */
    @NotNull
    private ParameterValue invalid(@NotNull ValidationResult result) {
        return new ParameterValue(null, result);
    }

    /**
     * Builds invalid parameter's value.
     *
     * @param error the validation error
     * @return invalid parameter's value
     */
    @NotNull
    private ParameterValue invalid(@Nullable String error) {
        return new ParameterValue(null, ValidationResult.error(error != null ? error : "unknown"));
    }

    /**
     * Returns valid empty parameter's value.
     *
     * @return valid parameter's value
     */
    @NotNull
    private ParameterValue empty() {
        return empty;
    }

    /**
     * Returns missed parameter's value.
     *
     * @return invalid parameter's value
     */
    @NotNull
    private ParameterValue missed() {
        return missed;
    }

    /**
     * Boolean parameter builder.
     *
     * @param name the name of parameter
     * @return builder
     */
    public static BooleanParameterBuilder booleanParameter(String name) {
        return new BooleanParameterBuilder(name);
    }

    /**
     * Short parameter builder.
     *
     * @param name the name of parameter
     * @return builder
     */
    public static ShortParameterBuilder shortParameter(String name) {
        return new ShortParameterBuilder(name);
    }

    /**
     * Integer parameter builder.
     *
     * @param name the name of parameter
     * @return builder
     */
    public static IntegerParameterBuilder integerParameter(String name) {
        return new IntegerParameterBuilder(name);
    }

    /**
     * Long parameter builder.
     *
     * @param name the name of parameter
     * @return builder
     */
    public static LongParameterBuilder longParameter(String name) {
        return new LongParameterBuilder(name);
    }

    /**
     * String parameter builder.
     *
     * @param name the name of parameter
     * @return builder
     */
    public static StringParameterBuilder stringParameter(String name) {
        return new StringParameterBuilder(name);
    }

    /**
     * String array parameter builder.
     *
     * @param name the name of parameter
     * @return builder
     */
    public static StringArrayParameterBuilder stringArrayParameter(String name) {
        return new StringArrayParameterBuilder(name);
    }

    /**
     * Enum parameter builder.
     * <p>
     * Parameter is case-sensitive.
     *
     * @param name     the name of parameter
     * @param enumType the enum type class
     * @param <E>      the enum type
     * @return builder
     */
    public static <E extends Enum<E>> EnumParameterBuilder<E> enumParameter(String name, Class<E> enumType) {
        return new EnumParameterBuilder<>(name, enumType);
    }

    /**
     * Parameter builder with parser.
     * <p>
     * Should be used for uncommon parameters of various types. In simple cases use appropriate parameter builders instead.
     *
     * @param name   the name of parameter
     * @param parser the parser
     * @param <T>    the type of parameter's value
     * @return builder
     * @see #booleanParameter(String)
     * @see #shortParameter(String)
     * @see #integerParameter(String)
     * @see #longParameter(String)
     * @see #stringParameter(String)
     * @see #stringArrayParameter(String)
     */
    public static <T> ParameterBuilder<T> parameter(String name, Parser<T> parser) {
        return new ParameterBuilder<>(name, parser);
    }

    public static class ParameterBuilder<T> {
        private final String name;
        private final Parser<T> parser;

        private ParameterType type = ParameterType.OPTIONAL;
        private T defaultValue = null;
        private Validator<T> validator = Validators.any();

        private ParameterBuilder(String name, Parser<T> parser) {
            this.name = name;
            this.parser = parser;
        }

        /**
         * The parameter is required.
         *
         * @return the same builder
         */
        public ParameterBuilder<T> required() {
            this.type = ParameterType.REQUIRED;

            return this;
        }

        /**
         * Use the {@code value} as the default parameter value. Also, parameter's type will be {@link ParameterType#DEFAULT}.
         *
         * @param value the default value
         * @return the same builder
         */
        public ParameterBuilder<T> withDefault(@NotNull T value) {
            this.defaultValue = value;
            this.type = ParameterType.DEFAULT;

            return this;
        }

        /**
         * Use the {@code validator} for parameter value.
         *
         * @param validator the validator
         * @return the same builder
         */
        public ParameterBuilder<T> withValidator(@NotNull Validator<T> validator) {
            this.validator = validator;

            return this;
        }

        /**
         * Build parameter.
         *
         * @return the parameter
         * @throws IllegalStateException if the parameter of type {@link ParameterType#DEFAULT} and default value if invalid
         */
        public Parameter<T> build() {
            if (type == ParameterType.DEFAULT) {
                ValidationResult result = validator.validate(defaultValue);
                if (result.isError()) {
                    throw new IllegalStateException(result.error());
                }
            }

            return new Parameter<>(name, type, defaultValue, parser, validator);
        }
    }

    public static class BooleanParameterBuilder extends ParameterBuilder<Boolean> {
        private BooleanParameterBuilder(String name) {
            super(name, Parsers.forBoolean());
        }
    }

    public static class ShortParameterBuilder extends ParameterBuilder<Short> {
        private ShortParameterBuilder(String name) {
            super(name, Parsers.forShort());
        }
    }

    public final static class IntegerParameterBuilder extends ParameterBuilder<Integer> {
        private IntegerParameterBuilder(String name) {
            super(name, Parsers.forInteger());
        }
    }

    public static class LongParameterBuilder extends ParameterBuilder<Long> {
        private LongParameterBuilder(String name) {
            super(name, Parsers.forLong());
        }
    }

    public static class StringParameterBuilder extends ParameterBuilder<String> {
        private StringParameterBuilder(String name) {
            super(name, Parsers.forString());
        }
    }

    public static class StringArrayParameterBuilder extends ParameterBuilder<String[]> {
        private StringArrayParameterBuilder(String name) {
            super(name, Parsers.forStringArray());
        }
    }

    public static class EnumParameterBuilder<E extends Enum<E>> extends ParameterBuilder<E> {
        private EnumParameterBuilder(String name, Class<E> enumType) {
            super(name, Parsers.forEnum(enumType));
        }
    }

    /**
     * The value of {@link Parameter}
     *
     * @author Gregory Koshelev
     */
    public class ParameterValue {
        private final T value;
        private final ValidationResult result;

        private ParameterValue(T value, ValidationResult result) {
            this.value = value;
            this.result = result;
        }

        /**
         * Returns valid non null value. Otherwise throws exception.
         *
         * @return the value of type {@link T}
         * @throws IllegalStateException if value is empty
         * @throws IllegalStateException if value is invalid
         */
        @NotNull
        public T get() {
            if (isEmpty()) {
                throw new IllegalStateException("Value for parameter '" + name + "' is empty");
            }

            if (result.isOk()) {
                return value;
            }

            throw new IllegalStateException("Value for parameter '" + name + "' is invalid: " + result.error());
        }

        /**
         * Returns non null value if it exists or {@code other} if {@link ParameterValue#isEmpty()}. Otherwise throws exception.
         *
         * @param other the other value to return if {@link ParameterValue#isEmpty()}
         * @return the value of type {@link T}
         * @throws IllegalStateException if value is invalid
         */
        @Nullable
        public T orEmpty(@Nullable T other) {
            if (isEmpty()) {
                return other;
            }

            if (isOk()) {
                return value;
            }

            throw new IllegalStateException("Value for parameter '" + name + "' is invalid: " + result.error());
        }

        /**
         * Returns validation result.
         *
         * @return validation result
         */
        @NotNull
        public ValidationResult result() {
            return result;
        }

        /**
         * Returns {@code true} if value is valid.
         *
         * @return {@code true} if value is valid
         */
        public boolean isOk() {
            return result.isOk();
        }

        /**
         * Returns {@code true} if value is invalid.
         *
         * @return {@code true} if value is invalid
         */
        public boolean isError() {
            return result.isError();
        }

        /**
         * Returns {@code true} if value is empty and it is acceptable.
         *
         * @return {@code true} if value is empty and it is acceptable
         */
        public boolean isEmpty() {
            return this == empty;
        }
    }
}