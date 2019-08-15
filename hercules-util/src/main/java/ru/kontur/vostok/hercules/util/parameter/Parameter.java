package ru.kontur.vostok.hercules.util.parameter;

import org.jetbrains.annotations.NotNull;
import ru.kontur.vostok.hercules.util.parameter.parsing.Parser;
import ru.kontur.vostok.hercules.util.parameter.parsing.Parsers;
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
    private final String name;
    private final ParameterType type;
    private final ParameterValue<T> defaultValue;
    private final Parser<T> parser;
    private final Validator<T> validator;

    private Parameter(String name, ParameterType type, ParameterValue<T> defaultValue, @NotNull Parser<T> parser, Validator<T> validator) {
        this.name = name;
        this.type = type;
        this.defaultValue = defaultValue;
        this.parser = parser;
        this.validator = validator;
    }

    /**
     * Extracts parameter's value from the string.
     *
     * @param string the string
     * @return extracted value
     */
    public ParameterValue<T> from(String string) {
        ParameterValue<T> parsed = parser.parse(string);

        if (!parsed.isOk()) {
            return parsed;
        }

        if (parsed.isEmpty()) {
            if (type == ParameterType.OPTIONAL) {
                return parsed;
            }

            if (type == ParameterType.REQUIRED) {
                return ParameterValue.ofNull();
            }

            return defaultValue;
        }

        ValidationResult result = validator.validate(parsed.get());
        if (result.isOk()) {
            return parsed;
        }
        return ParameterValue.invalid(result);
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

    public static class ParameterBuilder<T> {
        private String name;
        private ParameterType type = ParameterType.OPTIONAL;
        private T defaultValue = null;
        private Parser<T> parser;
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
        public ParameterBuilder<T> withDefault(T value) {
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

            return new Parameter<>(name, type, defaultValue != null ? ParameterValue.of(defaultValue) : ParameterValue.empty(), parser, validator);
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
}