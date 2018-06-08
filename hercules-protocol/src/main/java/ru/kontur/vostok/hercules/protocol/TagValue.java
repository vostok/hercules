package ru.kontur.vostok.hercules.protocol;

/**
 * @author Gregory Koshelev
 */
public class TagValue {
    private final Type type;
    private final Object value;

    public TagValue(Type type, Object value) {
        this.type = type;
        this.value = value;
    }

    public Type getType() {
        return type;
    }

    public Object getValue() {
        return value;
    }
}
