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
    private final Map<String, Variant> fields = new HashMap<>();

    public ContainerBuilder field(String key, Variant value) {
        fields.put(key, value);
        return this;
    }

    public Container build() {
        if (wasBuilt) {
            throw new IllegalStateException("Was already built");
        }
        wasBuilt = true;

        return new Container(fields);
    }

    public static ContainerBuilder create() {
        return new ContainerBuilder();
    }

    private ContainerBuilder() {
    }
}
