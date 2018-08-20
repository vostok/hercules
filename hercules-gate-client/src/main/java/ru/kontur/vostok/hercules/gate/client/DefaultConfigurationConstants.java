package ru.kontur.vostok.hercules.gate.client;

/**
 * @author Daniil Zhenikhov
 */
public final class DefaultConfigurationConstants {
    public static final int DEFAULT_BATCH_SIZE = 100;
    public static final int DEFAULT_CAPACITY = 10_000_000;
    public static final long DEFAULT_PERIOD_MILLIS = 1000;
    public static  final boolean DEFAULT_IS_LOSE_ON_OVERFLOW = false;

    private DefaultConfigurationConstants() {

    }
}
