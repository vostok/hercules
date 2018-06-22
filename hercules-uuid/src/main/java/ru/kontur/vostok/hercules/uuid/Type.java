package ru.kontur.vostok.hercules.uuid;

/**
 * @author Gregory Koshelev
 */
public class Type {
    public static final Type CLIENT = new Type(0b1001L);
    public static final Type INTERNAL = new Type(0b1000L);

    private final long type;

    private Type(long type) {
        this.type = type;
    }

    public long get() {
        return type;
    }
}
