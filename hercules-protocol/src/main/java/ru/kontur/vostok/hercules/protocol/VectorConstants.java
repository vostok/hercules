package ru.kontur.vostok.hercules.protocol;

public final class VectorConstants {

    public static final int VECTOR_MAX_LENGTH = 256; // FIXME: Rename constant
    public static final String VECTOR_LENGTH_ERROR_MESSAGE = "Vector length must be lesser than " + VECTOR_MAX_LENGTH + " items";
    public static final String STRING_LENGTH_ERROR_MESSAGE = "String bytes length must be lesser than " + VECTOR_MAX_LENGTH;
    public static final String STRING_LIST_ERROR_MESSAGE_TEMPLATE = STRING_LENGTH_ERROR_MESSAGE + ". Illegal string index [%d]";

    private VectorConstants() {
    }
}
