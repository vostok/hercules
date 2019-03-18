package ru.kontur.vostok.hercules.util.properties;

import ru.kontur.vostok.hercules.util.parsing.Parser;
import ru.kontur.vostok.hercules.util.validation.Validator;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * PropertyDescriptionBuilder - helper class to create property description via fluent interface
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

    /**
     * Start property description
     *
     * @param name property name
     * @param type property type
     * @param parser property parser, can be null and in this case it should be defined later
     * @return PropertyDescriptionBuilder
     */
    public static <T> PropertyDescriptionBuilder<T> start(String name, Class<T> type, Parser<T> parser) {
        return new PropertyDescriptionBuilder<>(name, type, parser);
    }

    /**
     * Use parser for parse value
     * @param parser value parser
     * @return PropertyDescriptionBuilder
     */
    public PropertyDescriptionBuilder<T> withParser(Parser<T> parser) {
        this.parser = parser;
        return this;
    }

    /**
     * Add value validator to validators list
     * @param validator value validator
     * @return PropertyDescriptionBuilder
     */
    public PropertyDescriptionBuilder<T> withValidator(Validator<T> validator) {
        if (validators.isEmpty()) {
            validators = new LinkedList<>();
        }
        validators.add(validator);
        return this;
    }

    /**
     * Add default value and mark property optional
     * @param defaultValue default value
     * @return PropertyDescriptionBuilder
     */
    public PropertyDescriptionBuilder<T> withDefaultValue(T defaultValue) {
        this.required = false;
        this.defaultValue = defaultValue;
        return this;
    }

    /**
     * Finish property description building and return immutable property description
     * @return PropertyDescription
     */
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
