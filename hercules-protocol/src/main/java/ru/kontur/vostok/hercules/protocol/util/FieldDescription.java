package ru.kontur.vostok.hercules.protocol.util;

import ru.kontur.vostok.hercules.protocol.Type;
import ru.kontur.vostok.hercules.util.arguments.Preconditions;

/**
 * FieldDescription stores field name and type
 *
 * @author Kirill Sulim
 */
public class FieldDescription {

    private final String name;
    private final Type type;

    private FieldDescription(String name, Type type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public Type getType() {
        return type;
    }

    public static FieldDescription create(String name, Type type) {
        Preconditions.checkNotNull(name);
        Preconditions.check(!name.isEmpty(), "Field name must be present");

        Preconditions.checkNotNull(type);

        return new FieldDescription(name, type);
    }
}
