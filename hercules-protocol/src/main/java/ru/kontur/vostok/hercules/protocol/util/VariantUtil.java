package ru.kontur.vostok.hercules.protocol.util;

import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Type;
import ru.kontur.vostok.hercules.protocol.Variant;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

public final class VariantUtil {

    private VariantUtil() {
    }

    public static boolean isPrimitive(Variant variant) {
        switch (variant.getType()) {
            case BYTE:
            case SHORT:
            case INTEGER:
            case LONG:
            case FLAG:
            case FLOAT:
            case DOUBLE:
            case UUID:
            case STRING:
            case NULL:
                return true;
            default:
                return false;
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
            case UUID:
                return Optional.of(String.valueOf(variant.getValue()));
            case STRING:
                return Optional.of(new String((byte[]) variant.getValue(), StandardCharsets.UTF_8));
            default:
                return Optional.empty();
        }
    }

    public static Optional<Container> extractContainer(Variant variant) {
        if (variant.getType() == Type.CONTAINER) {
            return Optional.of((Container) variant.getValue());
        }
        return Optional.empty();
    }
}
