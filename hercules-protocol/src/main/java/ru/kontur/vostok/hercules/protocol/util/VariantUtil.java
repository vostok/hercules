package ru.kontur.vostok.hercules.protocol.util;

import ru.kontur.vostok.hercules.protocol.Type;
import ru.kontur.vostok.hercules.protocol.Variant;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;

public final class VariantUtil {

    private VariantUtil() {
    }

    public static <T> Optional<T> extractRegardingType(Variant variant, Type type) {
        if (Objects.isNull(variant)) {
            return Optional.empty();
        }
        if (type != variant.getType()) {
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

    public static Optional<String> extractPrimitiveAsString(Variant variant) {
        switch (variant.getType()) {
            case BYTE:
            case SHORT:
            case INTEGER:
            case LONG:
            case FLAG:
            case FLOAT:
            case DOUBLE:
                return Optional.of(String.valueOf(variant.getValue()));
            case STRING:
            case TEXT:
                return Optional.of(new String((byte[]) variant.getValue(), StandardCharsets.UTF_8));
            default:
                return Optional.empty();
        }
    }
}
