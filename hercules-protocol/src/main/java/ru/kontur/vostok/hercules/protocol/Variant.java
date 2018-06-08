package ru.kontur.vostok.hercules.protocol;

public class Variant {

    private final Type type;
    private final Object value;

    public Variant(Type type, Object value) {
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
