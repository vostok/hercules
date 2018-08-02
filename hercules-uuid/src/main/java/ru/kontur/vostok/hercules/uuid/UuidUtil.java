package ru.kontur.vostok.hercules.uuid;

/**
 * UUID version 1 util class
 * @author Gregory Koshelev
 */
public final class UuidUtil {
    /**
     * Make most sig bits (hi 64 bits) for UUID version 1
     * @param ticks is 100ns ticks since Gregorian Epoch
     * @return hi 64 bits of UUID
     */
    public static long makeMostSigBits(long ticks) {
        return (0x00000000FFFFFFFFL & ticks) << 32// timestamp_low
                | (0x0000FFFF00000000L & ticks) >>> 16// timestamp_mid
                | (0x0FFF000000000000L & ticks) >>> 48// timestamp_hi
                | 0x0000000000001000L;// version 1
    }

    private UuidUtil() {

    }
}
