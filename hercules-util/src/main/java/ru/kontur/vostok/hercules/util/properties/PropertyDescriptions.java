package ru.kontur.vostok.hercules.util.properties;

import ru.kontur.vostok.hercules.util.parameter.parsing.Parser;
import ru.kontur.vostok.hercules.util.parsing.Parsers;

import java.util.List;
import java.util.Set;

/**
 * PropertyDescriptions - collections of util method to create property description
 *
 * @author Kirill Sulim
 */
@Deprecated
public final class PropertyDescriptions {

    /**
     * Starts description of property of type short
     * @param name name of property
     * @return PropertyDescriptionBuilder
     * @deprecated use {@link ru.kontur.vostok.hercules.util.parameter.Parameter#shortParameter(String)}
     */
    @Deprecated
    public static PropertyDescriptionBuilder<Short> shortProperty(String name) {
        return PropertyDescriptionBuilder.start(name, Short.class, Parsers::parseShort);
    }

    /**
     * Starts description of property of type integer
     * @param name name of property
     * @return PropertyDescriptionBuilder
     * @deprecated use {@link ru.kontur.vostok.hercules.util.parameter.Parameter#integerParameter(String)}
     */
    @Deprecated
    public static PropertyDescriptionBuilder<Integer> integerProperty(String name) {
        return PropertyDescriptionBuilder.start(name, Integer.class, Parsers::parseInteger);
    }

    /**
     * Starts description of property of type long
     * @param name name of property
     * @return PropertyDescriptionBuilder
     * @deprecated use {@link ru.kontur.vostok.hercules.util.parameter.Parameter#longParameter(String)}
     */
    @Deprecated
    public static PropertyDescriptionBuilder<Long> longProperty(String name) {
        return PropertyDescriptionBuilder.start(name, Long.class, Parsers::parseLong);
    }

    /**
     * Starts description of property of type string
     * @param name name of property
     * @return PropertyDescriptionBuilder
     * @deprecated use {@link ru.kontur.vostok.hercules.util.parameter.Parameter#stringParameter(String)}
     */
    @Deprecated
    public static PropertyDescriptionBuilder<String> stringProperty(String name) {
        return PropertyDescriptionBuilder.start(name, String.class, Parsers::parseString);
    }

    /**
     * Starts description of property of type boolean
     * @param name name of property
     * @return PropertyDescriptionBuilder
     * @deprecated use {@link ru.kontur.vostok.hercules.util.parameter.Parameter#booleanParameter(String)}
     */
    @Deprecated
    public static PropertyDescriptionBuilder<Boolean> booleanProperty(String name) {
        return PropertyDescriptionBuilder.start(name, Boolean.class, Parsers::parseBoolean);
    }

    /**
     * Starts description of property of type {@code clazz}
     * @param clazz type of property
     * @param name name of property
     * @return PropertyDescriptionBuilder
     * @deprecated use {@link ru.kontur.vostok.hercules.util.parameter.Parameter#parameter(String, Parser)}
     */
    @Deprecated
    public static <T> PropertyDescriptionBuilder<T> propertyOfType(Class<T> clazz, String name) {
        return PropertyDescriptionBuilder.start(name, clazz, null);
    }

    /**
     * Starts description of property of list of strings
     * @param name name of property
     * @return PropertyDescriptionBuilder
     * @deprecated use {@link ru.kontur.vostok.hercules.util.parameter.Parameter#booleanParameter(String)}
     */
    @Deprecated
    public static PropertyDescriptionBuilder<List<String>> listOfStringsProperty(String name) {
        // Trick to work-around type erasure
        // https://stackoverflow.com/a/30754982/2535153
        @SuppressWarnings("unchecked")
        Class<List<String>> clazz = (Class<List<String>>)(Class<?>)List.class;
        return PropertyDescriptionBuilder.start(name, clazz, Parsers.parseList(Parsers::parseString));
    }

    /**
     * Starts description of property of set of strings
     * @param name name of property
     * @return PropertyDescriptionBuilder
     * @deprecated use {@link ru.kontur.vostok.hercules.util.parameter.Parameter#booleanParameter(String)}
     */
    @Deprecated
    public static PropertyDescriptionBuilder<Set<String>> setOfStringsProperty(String name) {
        // Trick to work-around type erasure
        // https://stackoverflow.com/a/30754982/2535153
        @SuppressWarnings("unchecked")
        Class<Set<String>> clazz = (Class<Set<String>>)(Class<?>)Set.class;
        return PropertyDescriptionBuilder.start(name, clazz, Parsers.parseSet(Parsers::parseString));
    }

    /**
     * Starts description of property of array of strings
     * @param name name of property
     * @return PropertyDescriptionBuilder
     * @deprecated use {@link ru.kontur.vostok.hercules.util.parameter.Parameter#stringArrayParameter(String)}
     */
    @Deprecated
    public static PropertyDescriptionBuilder<String[]> arrayOfStringsProperty(String name) {
        return PropertyDescriptionBuilder.start(name, String[].class, Parsers.parseArray(String.class, Parsers::parseString));
    }

    private PropertyDescriptions() {
        /* static class */
    }
}
