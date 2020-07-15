package ru.kontur.vostok.hercules.json.transformer;

import ru.kontur.vostok.hercules.protocol.Variant;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * @author Gregory Koshelev
 */
@FunctionalInterface
public interface Transformer {
    Transformer PLAIN = new PlainTransformer();

    Object transform(Variant value);

    static Transformer fromClass(String className) {
        Class<?> transformerClass;
        try {
            transformerClass = Class.forName(className);
        } catch (ClassNotFoundException ex) {
            throw new IllegalArgumentException("Unknown class '" + className + "'", ex);
        }
        if (!Transformer.class.isAssignableFrom(transformerClass)) {
            throw new IllegalArgumentException("Class '" + className + "' is not inheritor of " + Transformer.class.getName());
        }

        Constructor<?> constructor;
        try {
            constructor = transformerClass.getConstructor();
        } catch (NoSuchMethodException ex) {
            throw new IllegalArgumentException("Cannot get default constructor for transformer class '" + className + "'", ex);
        }

        try {
            return (Transformer) constructor.newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException ex) {
            throw new IllegalArgumentException("Failed to init transformer from class '" + className + "'", ex);
        }
    }
}
