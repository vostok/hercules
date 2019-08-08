package ru.kontur.vostok.hercules.protocol.util;

import ru.kontur.vostok.hercules.protocol.Variant;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

public final class VariantUtil {

    private VariantUtil() {
    }

    public static Optional<String> extractAsString(Variant variant) {
        switch (variant.getType()) {
            case BYTE:
            case SHORT:
            case INTEGER:
            case LONG:
            case FLAG:
            case FLOAT:
            case DOUBLE:
            case NULL:
                return Optional.of(String.valueOf(variant.getValue()));
            case STRING:
                return Optional.of(new String((byte[]) variant.getValue(), StandardCharsets.UTF_8));
            case UUID:
                return Optional.of(variant.getValue().toString());
            default:
                return Optional.empty();
        }
    }
}
