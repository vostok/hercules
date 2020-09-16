package ru.kontur.vostok.hercules.protocol;

import java.util.Arrays;

/**
 * @author Gregory Koshelev
 */
public enum Type {
    TYPE(0x00, 1),
    CONTAINER(0x01, -1),
    BYTE(0x02, 1),
    SHORT(0x03, 2),
    INTEGER(0x04, 4),
    LONG(0x05, 8),
    FLAG(0x06, 1),
    FLOAT(0x07, 4),
    DOUBLE(0x08, 8),
    STRING(0x09, -1),
    UUID(0x0A, 16),
    NULL(0x0B, 0),
    VECTOR(0x80, -1),
    RESERVED(0xFF, -1);

    public final int code;
    /**
     * Size in bytes.
     * <p>
     * Size -1 means that value of that type has no fixed size.
     */
    public final int size;

    Type(int code, int size) {
        this.code = code;
        this.size = size;
    }

    public static Type valueOf(int code) {
        return CODES[code & 0xFF];
    }

    private static final Type[] CODES = new Type[256];

    static {
        Arrays.fill(CODES, RESERVED);

        for (Type type : values()) {
            if (RESERVED == type) {
                continue; // Skip RESERVED cause it has special code
            }
            CODES[type.code] = type;
        }
    }
}
