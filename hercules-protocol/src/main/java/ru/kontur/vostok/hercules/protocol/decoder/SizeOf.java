package ru.kontur.vostok.hercules.protocol.decoder;

public final class SizeOf {
    public final int BYTE = 1;
    public final int SHORT = 2;
    public final int INTEGER = 4;
    public final int LONG = 8;
    public final int FLAG = 1;
    public final int FLOAT = 4;
    public final int DOUBLE = 8;
    public final int STRING_LENGTH = 1;
    public final int TEXT_LENGTH = 4;
    public final int VECTOR_LENGTH = 1;
    public final int ARRAY_LENGTH = 4;

    
    private SizeOf() {
    }
}
