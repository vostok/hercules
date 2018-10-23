package ru.kontur.vostok.hercules.util.properties;

import ru.kontur.vostok.hercules.util.parsing.Parsers;

import java.util.List;

/**
 * PropertyDescriptions
 *
 * @author Kirill Sulim
 */
public final class PropertyDescriptions {

    public static PropertyDescriptionBuilder<Integer> integerProperty(String name) {
        return PropertyDescriptionBuilder.start(name, Integer.class, Parsers::parseInteger);
    }

    public static PropertyDescriptionBuilder<Long> longProperty(String name) {
        return PropertyDescriptionBuilder.start(name, Long.class, Parsers::parseLong);
    }

    public static PropertyDescriptionBuilder<String> stringProperty(String name) {
        return PropertyDescriptionBuilder.start(name, String.class, Parsers::parseString);
    }

    public static PropertyDescriptionBuilder<Boolean> booleanProperty(String name) {
        return PropertyDescriptionBuilder.start(name, Boolean.class, Parsers::parseBoolean);
    }

    public static <T> PropertyDescriptionBuilder<T> propertyOfType(Class<T> clazz, String name) {
        return PropertyDescriptionBuilder.start(name, clazz, null);
    }

    public static PropertyDescriptionBuilder<List<String>> listOfStringsProperty(String name) {
        // Trick to work-around type erasure
        // https://stackoverflow.com/a/30754982/2535153
        @SuppressWarnings("unchecked")
        Class<List<String>> clazz = (Class<List<String>>)(Class<?>)List.class;
        return PropertyDescriptionBuilder.start(name, clazz, Parsers.parseList(Parsers::parseString));
    }

    public static PropertyDescriptionBuilder<String[]> arrayOfStringsProperty(String name) {
        return PropertyDescriptionBuilder.start(name, String[].class, Parsers.parseArray(String.class, Parsers::parseString));
    }
}
