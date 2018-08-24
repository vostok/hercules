package ru.kontur.vostok.hercules.protocol.util;

import ru.kontur.vostok.hercules.protocol.Type;
import ru.kontur.vostok.hercules.util.arguments.ArgumentChecker;

/**
 * FieldDescription
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
        ArgumentChecker.check(name).isNotEmpty();
        ArgumentChecker.check(type).isNotNull();

        return new FieldDescription(name, type);
    }
}
