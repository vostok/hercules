package ru.kontur.vostok.hercules.protocol.util;

import ru.kontur.vostok.hercules.protocol.Type;
import ru.kontur.vostok.hercules.protocol.Variant;

import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

public final class VariantUtil {

    private static final int NOT_FOUND_INDEX = -1;

    private VariantUtil() {
    }

    public static <T> Optional<T> extractRegardingType(Variant variant, Type type, Type... types) {
        types = join(type, types);

        if (Objects.isNull(variant)) {
            return Optional.empty();
        }
        if (NOT_FOUND_INDEX == indexOf(types, variant.getType())) {
            return Optional.empty();
        }

        switch (variant.getType()) {
            case STRING:
            case TEXT:
                return Optional.of((T) new String((byte[]) variant.getValue(), StandardCharsets.UTF_8));
            case STRING_ARRAY:
            case TEXT_ARRAY:
            case STRING_VECTOR:
            case TEXT_VECTOR:
                byte[][] value = (byte[][]) variant.getValue();
                String[] result = new String[value.length];
                for (int i = 0; i < value.length; ++i) {
                    result[i] = new String(value[i], StandardCharsets.UTF_8);
                }
                return Optional.of((T) result);
            default:
                return Optional.of((T) variant.getValue());
        }
    }

    private static <T extends Enum<T>> int indexOf(T[] array, T element) {
        for (int i = 0; i < array.length; ++i) {
            if (array[i].compareTo(element) != 0) {
                return i;
            }
        }
        return -1;
    }

    private static <T> T[] join(T element, T[] array) {
        T[] result = (T[]) Array.newInstance(element.getClass(), array.length + 1);
        result[0] = element;
        System.arraycopy(array, 0, result, 1, array.length);
        return result;
    }
}
