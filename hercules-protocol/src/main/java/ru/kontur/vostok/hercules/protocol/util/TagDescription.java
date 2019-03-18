package ru.kontur.vostok.hercules.protocol.util;

import ru.kontur.vostok.hercules.protocol.Type;

import java.util.Collections;
import java.util.Map;
import java.util.function.Function;

/**
 * TagDescription stores tag name and type
 *
 * @author Kirill Sulim
 */
public class TagDescription<T> {

    private final String name;
    private final Map<Type, Function<Object, ? extends T>> extractors;

    public TagDescription(String name, Map<Type, Function<Object, ? extends T>> extractors) {
        this.name = name;
        this.extractors = Collections.unmodifiableMap(extractors);
    }

    public String getName() {
        return name;
    }

    public Map<Type, Function<Object, ? extends T>> getExtractors() {
        return extractors;
    }
}
