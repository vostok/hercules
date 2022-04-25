package ru.kontur.vostok.hercules.util.random;

import ru.kontur.vostok.hercules.util.bytes.ByteUtil;

import java.security.SecureRandom;

/**
 * @author Gregory Koshelev
 */
public final class RandomUtil {
    private static final SecureRandom ROOT = new SecureRandom();

    public static byte[] generateSeed(int numBytes) {
        return ROOT.generateSeed(numBytes);
    }

    public static long generateLongSeed() {
        return ByteUtil.toLong(generateSeed(8));
    }

    public static int generateIntSeed() {
        return ByteUtil.toInt(generateSeed(4));
    }

    private RandomUtil() {
        /* static class */
    }
}
