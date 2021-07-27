package ru.kontur.vostok.hercules.util;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.regex.Pattern;

/**
 * @author Gregory Koshelev
 */
public final class ClassUtil {
    private static final Pattern CLASS_NAME_REGEXP = Pattern.compile("(([a-zA-Z_$][a-zA-Z0-9_$]*\\.)*[a-zA-Z_$][a-zA-Z0-9_$]*)");

    private static final Class<?>[] EMPTY_PARAMETER_TYPES = new Class<?>[0];
    private static final Object[] EMPTY_PARAMETER_VALUES = new Object[0];

    /**
     * Create an instance of the class using default constructor.
     * <p>
     * The class must be an inheritor of the parent class.
     *
     * @param className   the class name
     * @param parentClass the parent class
     * @param <T>         the parent type
     * @return instance of the class
     */
    public static <T> T fromClass(@NotNull String className, @NotNull Class<T> parentClass) {
        return fromClass(className, parentClass, EMPTY_PARAMETER_TYPES, EMPTY_PARAMETER_VALUES);
    }

    /**
     * Create an instance of the class using a constructor with parameters.
     * <p>
     * The class must be an inheritor of the parent class.
     *
     * @param className       the class name
     * @param parentClass     the parent class
     * @param parameterTypes  parameter types
     * @param parameterValues parameter values
     * @param <T>             the parent type
     * @return instance of the class
     */
    @SuppressWarnings("unchecked")
    public static <T> T fromClass(@NotNull String className, @NotNull Class<T> parentClass, @NotNull Class<?>[] parameterTypes, @NotNull Object[] parameterValues) {
        if (!CLASS_NAME_REGEXP.matcher(className).matches()) {
            throw new IllegalArgumentException("Invalid class name '" + className + "'");
        }

        if (parameterTypes.length != parameterValues.length) {
            throw new IllegalArgumentException("Parameter types and values have different length");
        }

        Class<?> clazz;
        try {
            clazz = Class.forName(className);
        } catch (ClassNotFoundException ex) {
            throw new IllegalArgumentException("Unknown class '" + className + "'", ex);
        }
        if (!parentClass.isAssignableFrom(clazz)) {
            throw new IllegalArgumentException("Class '" + className + "' is not inheritor of " + parentClass.getName());
        }

        Constructor<?> constructor;
        try {
            constructor = clazz.getConstructor(parameterTypes);
        } catch (NoSuchMethodException ex) {
            throw new IllegalArgumentException("Cannot get default constructor for class '" + className + "'", ex);
        }

        try {
            return (T) constructor.newInstance(parameterValues);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException ex) {
            throw new IllegalArgumentException("Failed to init object from class '" + className + "'", ex);
        }
    }

    private ClassUtil() {
        /* static class */
    }
}
