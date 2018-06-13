package ru.kontur.vostok.hercules.protocol.decoder;

public final class SizeOf {
    public static final int BYTE = 1;
    public static final int SHORT = 2;
    public static final int INTEGER = 4;
    public static final int LONG = 8;
    public static final int FLAG = 1;
    public static final int FLOAT = 4;
    public static final int DOUBLE = 8;
    public static final int STRING_LENGTH = 1;
    public static final int TEXT_LENGTH = 4;
    public static final int VECTOR_LENGTH = 1;
    public static final int ARRAY_LENGTH = 4;


    private SizeOf() {
    }
}
