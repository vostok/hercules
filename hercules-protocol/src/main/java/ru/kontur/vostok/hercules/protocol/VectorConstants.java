package ru.kontur.vostok.hercules.protocol;

public final class VectorConstants {

    public static final int VECTOR_LENGTH_EXCEEDED = 256;
    public static final String VECTOR_LENGTH_ERROR_MESSAGE = "Vector length must be lesser than " + VECTOR_LENGTH_EXCEEDED + " items";
    public static final String STRING_LENGTH_ERROR_MESSAGE = "String bytes length must be lesser than " + VECTOR_LENGTH_EXCEEDED;
    public static final String STRING_LIST_ERROR_MESSAGE_TEMPLATE = STRING_LENGTH_ERROR_MESSAGE + ". Illegal string index [%d]";

    private VectorConstants() {
    }
}
