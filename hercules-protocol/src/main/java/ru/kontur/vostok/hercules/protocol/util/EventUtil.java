package ru.kontur.vostok.hercules.protocol.util;

import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.util.throwable.NotImplementedException;

import java.util.Collection;

/**
 * EventUtil
 *
 * @author Kirill Sulim
 */
public final class EventUtil {

    public static int getSizeInBytes(Event event) {
        return event.getBytes().length;
    }

    public static int getSizeInBytes(Collection<Event> events) {
        int size = 0;
        for (Event event : events) {
            size += event.getBytes().length;
        }
        return size;
    }

    public static String toString(final Event event) {
        throw new NotImplementedException("TODO: Add one line print and pretty print using ContainerUtil::toString");
    }

    private EventUtil() {
        /* Static class */
    }
}
