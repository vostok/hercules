package ru.kontur.vostok.hercules.util.properties;

import ru.kontur.vostok.hercules.util.parsing.Parser;
import ru.kontur.vostok.hercules.util.validation.Validator;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * PropertyDescriptionBuilder
 *
 * @author Kirill Sulim
 */
public class PropertyDescriptionBuilder<T> {

    private String name;
    private Parser<T> parser;

    private List<Validator<T>> validators = Collections.emptyList();

    private boolean required = true;
    private T defaultValue = null;

    private PropertyDescriptionBuilder(String name, Class<T> type, Parser<T> parser) {
        this.name = name;
        this.parser = parser;
    }

    public static <T> PropertyDescriptionBuilder<T> start(String name, Class<T> type, Parser<T> parser) {
        return new PropertyDescriptionBuilder<>(name, type, parser);
    }

    public PropertyDescriptionBuilder<T> withParser(Parser<T> parser) {
        this.parser = parser;
        return this;
    }

    public PropertyDescriptionBuilder<T> withValidator(Validator<T> validator) {
        if (validators.isEmpty()) {
            validators = new LinkedList<>();
        }
        validators.add(validator);
        return this;
    }

    public PropertyDescriptionBuilder<T> withDefaultValue(T defaultValue) {
        this.required = false;
        this.defaultValue = defaultValue;
        return this;
    }

    public PropertyDescription<T> build() {
        return new PropertyDescription<>(
                name,
                parser,
                validators,
                required,
                defaultValue
        );
    }
}
