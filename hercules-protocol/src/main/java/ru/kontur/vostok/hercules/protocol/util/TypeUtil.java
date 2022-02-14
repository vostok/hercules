package ru.kontur.vostok.hercules.protocol.util;

import ru.kontur.vostok.hercules.protocol.TinyString;
import ru.kontur.vostok.hercules.protocol.Type;

import java.util.UUID;

/**
 * Utility class for {@link Type}-related operations.
 *
 * @author Anton Akkuzin
 */
public final class TypeUtil {

    /**
     * Parses a value from {@link String} by the specified primitive {@link Type}.
     *
     * @param value {@link String} value to parse
     * @param type  the type by which the {@code value} should be parsed
     * @return parsed {@code value} boxed in {@link Object}.
     */
    public static Object parsePrimitiveValue(String value, Type type) {
        switch (type) {
            case BYTE:
                return Byte.parseByte(value);
            case SHORT:
                return Short.parseShort(value);
            case INTEGER:
                return Integer.parseInt(value);
            case LONG:
                return Long.parseLong(value);
            case FLAG:
                return Boolean.parseBoolean(value);
            case FLOAT:
                return Float.parseFloat(value);
            case DOUBLE:
                return Double.parseDouble(value);
            case UUID:
                return UUID.fromString(value);
            case STRING:
                return TinyString.of(value);
            case NULL:
                if (value.equalsIgnoreCase("null")) {
                    return null;
                }
                throw new IllegalArgumentException(String.format("Can't parse value with Type.%s from string '%s'", type, value));
            default:
                throw new IllegalArgumentException(String.format("Type '%s' is not primitive.", type));
        }
    }

    private TypeUtil() {
        /* static class */
    }
}
