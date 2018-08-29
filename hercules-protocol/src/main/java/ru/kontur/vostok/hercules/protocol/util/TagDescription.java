package ru.kontur.vostok.hercules.protocol.util;

import ru.kontur.vostok.hercules.protocol.Type;
import ru.kontur.vostok.hercules.util.arguments.Preconditions;

/**
 * TagDescription stores tag name and type
 *
 * @author Kirill Sulim
 */
public class TagDescription {

    private final String name;
    private final Type type;

    private TagDescription(String name, Type type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public Type getType() {
        return type;
    }

    public static TagDescription create(String name, Type type) {
        Preconditions.checkNotNull(name);
        Preconditions.check(!name.isEmpty(), "Tag name must be present");

        Preconditions.checkNotNull(type);

        return new TagDescription(name, type);
    }
}
