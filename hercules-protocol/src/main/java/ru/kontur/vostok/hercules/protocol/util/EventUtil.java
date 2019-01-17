package ru.kontur.vostok.hercules.protocol.util;

import ru.kontur.vostok.hercules.protocol.Event;

import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.UUID;

/**
 * @author Gregory Koshelev
 */
public class EventUtil {
    private static char[] HEX = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
    private static char ZERO = '0';

    private static final int ID_SIZE_IN_BYTES = 24;

    public static ByteBuffer eventIdAsByteBuffer(long timestamp, UUID random) {
        ByteBuffer eventId = ByteBuffer.allocate(ID_SIZE_IN_BYTES);
        eventId.putLong(timestamp);
        eventId.putLong(random.getMostSignificantBits());
        eventId.putLong(random.getLeastSignificantBits());
        eventId.position(0);
        return eventId;
    }

    public static byte[] eventIdAsBytes(long timestamp, UUID random) {
        return eventIdAsByteBuffer(timestamp, random).array();
    }

    public static String eventIdAsHexString(long timestamp, UUID random) {
        return eventIdOfBytesAsHexString(eventIdAsBytes(timestamp, random));
    }

    /**
     * Return min eventId in lexicographical sense for specified timestamp in form '0x' + timestamp_in_hex + '0' * 32
     * @param timestamp
     * @return
     */
    public static String minEventIdForTimestampAsHexString(long timestamp) {
        char[] eventIdHex = new char[50];
        eventIdHex[0] = '0';
        eventIdHex[1] = 'x';
        eventIdHex[2] = HEX[(int) (timestamp >>> 60) & 0x0F];
        eventIdHex[3] = HEX[(int) (timestamp >>> 56) & 0x0F];
        eventIdHex[4] = HEX[(int) (timestamp >>> 52) & 0x0F];
        eventIdHex[5] = HEX[(int) ((timestamp >> 48) & 0x0F)];
        eventIdHex[6] = HEX[(int) ((timestamp >> 44) & 0x0F)];
        eventIdHex[7] = HEX[(int) ((timestamp >> 40) & 0x0F)];
        eventIdHex[8] = HEX[(int) ((timestamp >> 36) & 0x0F)];
        eventIdHex[9] = HEX[(int) ((timestamp >> 32) & 0x0F)];
        eventIdHex[10] = HEX[(int) ((timestamp >> 28) & 0x0F)];
        eventIdHex[11] = HEX[(int) ((timestamp >> 24) & 0x0F)];
        eventIdHex[12] = HEX[(int) ((timestamp >> 20) & 0x0F)];
        eventIdHex[13] = HEX[(int) ((timestamp >> 16) & 0x0F)];
        eventIdHex[14] = HEX[(int) ((timestamp >> 12) & 0x0F)];
        eventIdHex[15] = HEX[(int) ((timestamp >> 8) & 0x0F)];
        eventIdHex[16] = HEX[(int) ((timestamp >> 4) & 0x0F)];
        eventIdHex[17] = HEX[(int) (timestamp & 0x0F)];

        for (int i = 18; i < 50; i++) {
            eventIdHex[i] = ZERO;
        }

        return new String(eventIdHex);
    }

    public static String eventIdOfBytesAsHexString(byte[] bytes) {
        if (bytes == null || bytes.length != ID_SIZE_IN_BYTES) {
            throw new IllegalArgumentException("Binary representation of event id must be " + ID_SIZE_IN_BYTES + " bytes length");
        }

        char[] eventIdHex = new char[ID_SIZE_IN_BYTES * 2 + 2];
        eventIdHex[0] = '0';
        eventIdHex[1] = 'x';
        for (int i = 0; i < ID_SIZE_IN_BYTES; i++) {
            eventIdHex[2 * i + 2] = HEX[(bytes[i] >> 4) & 0x0F];
            eventIdHex[2 * i + 3] = HEX[bytes[i] & 0x0F];
        }

        return new String(eventIdHex);
    }

    public static String extractStringId(final Event event) {
        return Base64.getEncoder().encodeToString(eventIdAsBytes(event.getTimestamp(), event.getUuid()));
    }

    private EventUtil() {
        /* static class */
    }
}
