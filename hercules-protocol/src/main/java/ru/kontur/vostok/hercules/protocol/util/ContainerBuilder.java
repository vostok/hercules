package ru.kontur.vostok.hercules.protocol.util;

import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Variant;

import java.util.HashMap;
import java.util.Map;

/**
 * ContainerBuilder - NOT thread-safe
 *
 * @author Kirill Sulim
 */
public class ContainerBuilder {

    private boolean wasBuilt = false;
    private final Map<String, Variant> tags = new HashMap<>();

    public ContainerBuilder tag(String key, Variant value) {
        tags.put(key, value);
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
