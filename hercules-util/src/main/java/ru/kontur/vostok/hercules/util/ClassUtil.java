package ru.kontur.vostok.hercules.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.regex.Pattern;

/**
 * @author Gregory Koshelev
 */
public final class ClassUtil {
    private static final Pattern CLASS_NAME_REGEXP = Pattern.compile("(([a-zA-Z_$][a-zA-Z0-9_$]*\\.)*[a-zA-Z_$][a-zA-Z0-9_$]*)");

    @SuppressWarnings("unchecked")
    public static <T> T fromClass(String className, Class<T> parentClass) {
        if (!CLASS_NAME_REGEXP.matcher(className).matches()) {
            throw new IllegalArgumentException("Invalid class name '" + className + "'");
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
            constructor = clazz.getConstructor();
        } catch (NoSuchMethodException ex) {
            throw new IllegalArgumentException("Cannot get default constructor for class '" + className + "'", ex);
        }

        try {
            return (T) constructor.newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException ex) {
            throw new IllegalArgumentException("Failed to init object from class '" + className + "'", ex);
        }
    }

    private ClassUtil() {
        /* static class */
    }
}
