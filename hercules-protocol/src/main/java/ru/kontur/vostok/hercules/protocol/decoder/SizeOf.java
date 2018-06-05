package ru.kontur.vostok.hercules.protocol.decoder;

public interface SizeOf {
    int BYTE = 1;
    int SHORT = 2;
    int INTEGER = 4;
    int LONG = 8;
    int FLAG = 1;
    int FLOAT = 4;
    int DOUBLE = 8;
    int STRING_LENGTH = 1;
    int TEXT_LENGTH = 4;
    int VECTOR_LENGTH = 1;
    int ARRAY_LENGTH = 4;
}
