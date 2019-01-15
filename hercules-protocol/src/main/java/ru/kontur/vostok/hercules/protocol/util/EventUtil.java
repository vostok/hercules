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

    private static final int UUID_SIZE = 16;
    private static final int SHORTENED_LENGTH = 22;

    public static String extractStringId(final Event event) {

        final ByteBuffer buffer = ByteBuffer.allocate(UUID_SIZE);
        buffer.putLong(event.getUuid().getMostSignificantBits());
        buffer.putLong(event.getUuid().getLeastSignificantBits());

        final String base64String = Base64.getEncoder().encodeToString(buffer.array());
        return event.getTimestamp() + "/" + base64String.substring(0, SHORTENED_LENGTH);
    }

    private EventUtil() {
        /* static class */
    }
}
