package ru.kontur.vostok.hercules.protocol;

/**
 * @author Daniil Zhenikhov
 */
public final class CommonConstants {
    public static final int MAX_MESSAGE_SIZE = 2 * 1024 * 1024;
    /**
     * Serialized batch contains at least event count (32 bit integer)
     */
    public static final int MIN_EVENT_BATCH_SIZE_IN_BYTES = 4;

    private CommonConstants() {
    }
}
