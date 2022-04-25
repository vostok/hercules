package ru.kontur.vostok.hercules.util.number;

/**
 * @author Gregory Koshelev
 */
public final class IntegerUtil {
    public static int toPositive(int value) {
        return value & 0x7FFFFFFF;
    }

    private IntegerUtil() {
        /* static class */
    }
}
