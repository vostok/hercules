package ru.kontur.vostok.hercules.protocol.util;

import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.Type;
import ru.kontur.vostok.hercules.protocol.Variant;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;

public final class TagExtractor {

    private static final int NOT_FOUND_INDEX = -1;

    private TagExtractor() {
    }

    public static Optional<String> extractString(Event event, String tagName) {
        if (Objects.isNull(event)) {
            return Optional.empty();
        }

        return Optional.ofNullable(
                extractRegardingType(event.getTag(tagName), Type.STRING, Type.TEXT)
        );
    }

    private static <T> T extractRegardingType(Variant variant, Type... types) {
        if (Objects.isNull(variant)) {
            return null;
        }
        if (NOT_FOUND_INDEX == indexOf(types, variant.getType())) {
            return null;
        }

        switch (variant.getType()) {
            case STRING:
            case TEXT:
                return (T) new String((byte[]) variant.getValue(), StandardCharsets.UTF_8);
            case STRING_ARRAY:
            case TEXT_ARRAY:
            case STRING_VECTOR:
            case TEXT_VECTOR:
                byte[][] value = (byte[][]) variant.getValue();
                String[] result = new String[value.length];
                for (int i = 0; i < value.length; ++i) {
                    result[i] = new String(value[i], StandardCharsets.UTF_8);
                }
                return (T) result;
            default:
                return (T) variant.getValue();
        }
    }

    private static <T> int indexOf(T[] array, T element) {
        for (int i = 0; i < array.length; ++i) {
            if (array[i] == element) {
                return i;
            }
        }
        return -1;
    }
}
