package ru.kontur.vostok.hercules.protocol.util;

import ru.kontur.vostok.hercules.protocol.Event;

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

    public static String toString(final Event event, final boolean pretty) {
        final StringBuilder sb = new StringBuilder(event.getBytes().length * 2);

        final String line = pretty ? "\n" : "";
        final String separator = "," + line;
        final String equals = pretty ? " = " : "=";
        final String indentString = pretty ? ContainerUtil.indentString(1) : "";

        sb.append("{").append(line);
        sb.append(indentString).append("version").append(equals).append(event.getVersion()).append(separator);
        sb.append(indentString).append("id").append(equals).append(event.getId()).append(separator);
        sb.append(indentString).append("payload").append(equals);
        ContainerUtil.toString(sb, event.getPayload(), pretty, 1);
        sb.append(separator);
        sb.append("}");

        return sb.toString();
    }

    private EventUtil() {
        /* Static class */
    }
}
