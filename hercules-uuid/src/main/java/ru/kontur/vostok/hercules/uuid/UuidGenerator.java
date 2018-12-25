package ru.kontur.vostok.hercules.uuid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.util.time.TimeUtil;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.SecureRandom;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static ru.kontur.vostok.hercules.uuid.UuidUtil.makeMostSigBits;

/**
 * UuidGenerator produces 128-bits UUID version 1.                                                                  <br>
 * <br>
 * UUIDs have the following format:                                                                                 <br>
 * UUID (128 bit) := mostSigBits | leastSigBits                                                                     <br>
 * mostSigBits (64 bit) := timestamp_low | timestamp_mid | version | timestamp_hi                                   <br>
 * leastSigBits (64 bit) := variant | type | rnd_hi | rnd_low | reserved                                            <br>
 * timestamp_low (32 bit)                                                                                           <br>
 * timestamp_mid (16 bit)                                                                                           <br>
 * timestamp_hi (12 bit)                                                                                            <br>
 * timestamp (60 bit) := timestamp_hi | timestamp_mid | timestamp_low                                               <br>
 * timestamp (60 bit) : 100ns-ticks from Gregorian Epoch (15 October 1582 00:00:00.000 UTC)                         <br>
 * version (4 bit) : 0b0001                                                                                         <br>
 * variant (2 bit) : 0b10 (to be compatible with UUID version 1)                                                    <br>
 * type (1 bit) : set type of UUID to be client-side (is equal to 0) or server-side (is equal to 1)                 <br>
 * rnd_hi (13 bit) : static random value                                                                            <br>
 * rnd_low (35 bit) : static random value                                                                           <br>
 * rnd (48 bit) := rnd_hi | rnd_low                                                                                 <br>
 * rnd (48 bit) : value is used to replace clock_seq and node in UUID version 1 specification                       <br>
 * reserved (13 bit) : resereved for further implementations. Must be equal to 0b0                                  <br>
 * @author Gregory Koshelev
 */
public class UuidGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(UuidGenerator.class);

    private static final AtomicLong LAST_TICKS = new AtomicLong(0L);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final long VARIANT_RND = makeVariantRnd();

    private static final UuidGenerator CLIENT_GENERATOR_INSTANCE = new UuidGenerator(Type.CLIENT);
    private static final UuidGenerator INTERNAL_GENERATOR_INSTANCE = new UuidGenerator(Type.INTERNAL);

    private final Type type;

    private UuidGenerator(Type type) {
        this.type = type;
    }

    public static UuidGenerator getClientInstance() {
        return CLIENT_GENERATOR_INSTANCE;
    }

    public static UuidGenerator getInternalInstance() {
        return INTERNAL_GENERATOR_INSTANCE;
    }

    /**
     * Get min UUID with ticks specified
     * @param ticks is 100ns ticks from Gregorian Epoch
     * @return min UUID
     */
    public static UUID min(long ticks) {
        return new UUID(makeMostSigBits(ticks), 0x8000000000000000L);
    }

    public UUID next() {
        return new UUID(makeMostSigBits(nextTimestamp()), makeLeastSigBits(type));
    }

    public UUID withTicks(long ticks) {
        return new UUID(makeMostSigBits(ticks), makeLeastSigBits(type));
    }

    private static long makeLeastSigBits(Type type) {
        return VARIANT_RND | (type.get() << 61);
    }

    private static long makeVariantRnd() {
        long rnd = RANDOM.nextLong();
        return 0x8000000000000000L// variant
                | ((rnd & 0x0000FFFFFFFFFFFFL) << 13)// rnd
                ;
    }

    private static long nextTimestamp() {
        while (true) {
            long nowMillis = System.currentTimeMillis();
            long nowTicks = TimeUtil.unixMillisToGregorianTicks(nowMillis);
            long highTicks = TimeUtil.unixMillisToGregorianTicks(nowMillis + 1);
            long lastTicks = UuidGenerator.LAST_TICKS.get();
            if (nowTicks > lastTicks) {
                if (UuidGenerator.LAST_TICKS.compareAndSet(lastTicks, nowTicks)) {
                    return nowTicks;
                }
                // Someone else jumped to another millisecond: go to next spin
            } else {
                if (highTicks > lastTicks) {
                    return UuidGenerator.LAST_TICKS.incrementAndGet();
                }
                // Run far ahead to next millisecond: go to next spin
            }
        }
    }

    private static long unixTimeToTimestamp(long millis) {
        return millis * 10000;
    }

    private static long timestampToUnixTime(long timestamp) {
        return timestamp / 10000;
    }

    private static long pid() {
        String jvmName = java.lang.management.ManagementFactory.getRuntimeMXBean().getName();
        String pidString = jvmName.substring(0, jvmName.indexOf('@'));
        return Long.valueOf(pidString);
    }

    private static long addr() {
        /* TODO: Find lowest IPv4 addr except 127.0.0.1; Otherwise use hash of lowest IPv6 addr;
        Also, it's possible to compute hash of all available addresses; */
        try {
            InetAddress addr = InetAddress.getLocalHost();
            byte[] bytes = addr.getAddress();
            if (bytes.length == 4) {
                return (bytes[0] << 24) | (bytes[1] << 16) | (bytes[2] << 8) | bytes[3];
            }
            return 0L;//TODO: IPv6 addresses also are possible.
        } catch (UnknownHostException e) {
            LOGGER.warn("Unknown host", e);
            return 0L;
        }
    }
}
