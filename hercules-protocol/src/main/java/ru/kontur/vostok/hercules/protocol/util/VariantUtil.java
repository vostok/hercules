package ru.kontur.vostok.hercules.protocol.util;

import ru.kontur.vostok.hercules.protocol.Type;
import ru.kontur.vostok.hercules.protocol.Variant;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;

public final class VariantUtil {

    private VariantUtil() {
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
                return Optional.of(new String((byte[]) variant.getValue(), StandardCharsets.UTF_8));
            default:
                return Optional.empty();
        }
    }
}
