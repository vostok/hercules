package ru.kontur.vostok.hercules.protocol.util;

import ru.kontur.vostok.hercules.protocol.Event;

/**
 * @author Gregory Koshelev
 */
public class EventFormatter {
    public static String format(final Event event, final boolean pretty) {
        final StringBuilder sb = new StringBuilder(event.getBytes().length * 2);

        final String line = pretty ? System.lineSeparator() : "";
        final String separator = "," + line;
        final String equals = pretty ? " = " : "=";
        final String indentString = pretty ? ContainerUtil.indentString(1) : "";

        sb.append("{").append(line);
        sb.append(indentString).append("version").append(equals).append(event.getVersion()).append(separator);
        sb.append(indentString).append("timestamp").append(equals).append(event.getTimestamp()).append(separator);
        sb.append(indentString).append("uuid").append(equals).append(event.getUuid()).append(separator);
        sb.append(indentString).append("payload").append(equals);
        ContainerUtil.toString(sb, event.getPayload(), pretty, 1);
        sb.append(separator);
        sb.append("}");

        return sb.toString();
    }

}
