package ru.kontur.vostok.hercules.uuid;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * UuidGenerator produces custom 128-bits UUID.                                                                     <br>
 * <br>
 * Generated UUIDs do not compatible with RFC4122 since                                                             <br>
 * there is no corresponding VERSION or VARIANT parts of standardized UUID.                                         <br>
 * <br>
 * UUIDs have the following format:                                                                                 <br>
 * UUID (128 bit) := mostSigBits | leastSigBits                                                                     <br>
 * mostSigBits (64 bit) := timestamp | seed                                                                         <br>
 * leastSigBits (64 bit) := type | marker | addr | pid                                                              <br>
 * timestamp (56 bit) : 100ns-ticks from UNIX Epoch (1 January 1970 00:00:00.000 UTC)                               <br>
 * seed (8 bit) : random generated seed                                                                             <br>
 * type (4 bit) : set type of UUID to be client-side (is equal to 9) or server-side (is equal to 8)                 <br>
 * marker (12 bit) : most significant bits of SHA-512 hash value of key which usually is apiKey                     <br>
 * addr (32 bit) : lowest IPv4 address (except 127.0.0.1), otherwise TODO use IPv6 instead                          <br>
 * pid (16 bit) : least significant bits of process id.                                                             <br>
 * @author Gregory Koshelev
 */
public class UuidGenerator {
    private static final int SIZEOF_TIMESTAMP = 56;
    private static final int SIZEOF_SEED = 8;
    private static final int SIZEOF_TYPE = 4;
    private static final int SIZEOF_MARKER = 12;
    private static final int SIZEOF_ADDR = 32;
    private static final int SIZEOF_PID = 16;

    private static final int OFFSET_TIMESTAMP = 8;
    private static final int OFFSET_SEED = 0;
    private static final int OFFSET_TYPE = 60;
    private static final int OFFSET_MARKER = 48;
    private static final int OFFSET_ADDR = 16;
    private static final int OFFSET_PID = 0;

    private static final long seed = new Random(System.currentTimeMillis()).nextLong() & 0x00000000000000FFL;
    private static final AtomicLong lastTimestamp = new AtomicLong(0L);

    private final long leastSigBits;

    private UuidGenerator(Type type) {
        leastSigBits = makeLeastSigBits(Marker.EMPTY.get(), type.get());
    }

    public static UuidGenerator getClientInstance() {
        return new UuidGenerator(Type.CLIENT);
    }

    public static UuidGenerator getInternalInstance() {
        return new UuidGenerator(Type.INTERNAL);
    }

    public UUID next(Marker marker) {
        return new UUID(makeMostSigBits(), leastSigBits | (marker.get() << OFFSET_MARKER));
    }

    private long makeMostSigBits() {
        return (nextTimestamp() << OFFSET_TIMESTAMP) | seed;
    }

    private long makeLeastSigBits(long marker, long type) {
        return (type << OFFSET_TYPE) | (marker << OFFSET_MARKER) | (addr() << OFFSET_ADDR) | (pid() & 0x000000000000FFFFL);
    }

    private static long nextTimestamp() {
        while (true) {
            long nowMillis = System.currentTimeMillis();
            long nowTimestamp = unixTimeToTimestamp(nowMillis);
            long highTimestamp = unixTimeToTimestamp(nowMillis + 1);
            long lastTimestamp = UuidGenerator.lastTimestamp.get();
            if (nowTimestamp > lastTimestamp) {
                if (UuidGenerator.lastTimestamp.compareAndSet(lastTimestamp, nowTimestamp)) {
                    return nowTimestamp;
                }
                // Someone else jumped to another millisecond: go to next spin
            } else {
                if (highTimestamp > lastTimestamp) {
                    return UuidGenerator.lastTimestamp.incrementAndGet();
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

    private static long addr() {//TODO: Find lowest IPv4 addr except 127.0.0.1; Otherwise use hash of lowest IPv6 addr; Also, it's possible to compute hash of all available addresses;
        try {
            InetAddress addr = InetAddress.getLocalHost();
            byte[] bytes = addr.getAddress();
            if (bytes.length == 4) {
                return (bytes[0] << 24) | (bytes[1] << 16) | (bytes[2] << 8) | bytes[3];
            }
            return 0L;//TODO: IPv6 addresses also are possible.
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return 0L;
        }
    }
}
