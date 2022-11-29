package ru.kontur.vostok.hercules.sentry.client.impl.client.compression;

import java.util.function.IntPredicate;

/**
 * @author Aleksandr Yuferov
 */
public final class CompressionStrategies {

    public static IntPredicate never() {
        return length -> false;
    }

    public static IntPredicate always() {
        return length -> true;
    }

    public static IntPredicate byThreshold(int threshold) {
        return length -> length >= threshold;
    }
}
