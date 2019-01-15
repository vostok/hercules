package ru.kontur.vostok.hercules.protocol.util;

import ru.kontur.vostok.hercules.protocol.Event;

import java.nio.ByteBuffer;
import java.util.Base64;

/**
 * EventUtil
 *
 * @author Kirill Sulim
 */
public final class EventUtil {

    private static final int ID_SIZE = 24;

    public static String extractStringId(final Event event) {

        final ByteBuffer buffer = ByteBuffer.allocate(ID_SIZE);
        buffer.putLong(event.getTimestamp());
        buffer.putLong(event.getUuid().getMostSignificantBits());
        buffer.putLong(event.getUuid().getLeastSignificantBits());

        return Base64.getEncoder().encodeToString(buffer.array());
    }

    private EventUtil() {
        /* static class */
    }
}
