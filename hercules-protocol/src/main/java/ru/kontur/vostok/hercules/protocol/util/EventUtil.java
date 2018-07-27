package ru.kontur.vostok.hercules.protocol.util;

import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.Type;

import java.util.Optional;

/**
 * EventUtil
 *
 * @author Kirill Sulim
 */
public class EventUtil {

    public static Optional<String> extractString(Event event, String tagName) {
        return EventUtil.extractOptional(event, tagName, Type.STRING, Type.TEXT);
    }

    public static <T> Optional<T> extractOptional(Event event, String tagName, Type type, Type... types) {
        return VariantUtil.extractRegardingType(event.getTag(tagName), type, types);
    }

    public static <T> T extractRequired(Event event, String tagName, Type type, Type... types) {
        return EventUtil.<T>extractOptional(event, tagName, type, types)
                .orElseThrow(() -> new IllegalArgumentException(String.format("Missing required tag '%s'", tagName)));
    }
}
