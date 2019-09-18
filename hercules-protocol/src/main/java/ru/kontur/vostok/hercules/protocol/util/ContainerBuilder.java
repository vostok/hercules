package ru.kontur.vostok.hercules.protocol.util;

import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Variant;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ContainerBuilder - NOT thread-safe
 *
 * @author Kirill Sulim
 */
public class ContainerBuilder {

    private boolean wasBuilt = false;
    private final Map<String, Variant> tags = new LinkedHashMap<>();

    public ContainerBuilder tag(String key, Variant value) {
        tags.put(key, value);
        return this;
    }

    public <T> ContainerBuilder tag(TagDescription<T> tag, Variant value) {
        if (!tag.getExtractors().containsKey(value.getType())) {
            String allowedValues = "[" + tag.getExtractors().keySet().stream().map(String::valueOf).collect(Collectors.joining(", ")) + "]";
            throw new IllegalArgumentException(
                    String.format("Value type mismatch, expected one of %s, actual: %s", allowedValues, value.getType())
            );
        }
        tag(tag.getName(), value);
        return this;
    }

    public Container build() {
        if (wasBuilt) {
            throw new IllegalStateException("Was already built");
        }
        wasBuilt = true;

        return new Container(tags);
    }

    public static ContainerBuilder create() {
        return new ContainerBuilder();
    }

    private ContainerBuilder() {
    }
}
