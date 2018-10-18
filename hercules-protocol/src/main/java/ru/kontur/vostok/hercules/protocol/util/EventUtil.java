package ru.kontur.vostok.hercules.protocol.util;

import ru.kontur.vostok.hercules.protocol.Event;

import java.util.Collection;

/**
 * EventUtil
 *
 * @author Kirill Sulim
 */
public final class EventUtil {

    public static int getSizeInBytes(Collection<Event> events) {
        int size = 0;
        for (Event event : events) {
            size += event.getBytes().length;
        }
        return size;
    }

    private EventUtil() {
        /* Static class */
    }
}
