package ru.kontur.vostok.hercules.protocol;

import java.util.Arrays;

/**
 * @author Gregory Koshelev
 */
public enum Type {
    TYPE(0x00, 1, false),
    CONTAINER(0x01, -1, false),
    BYTE(0x02, 1, true),
    SHORT(0x03, 2, true),
    INTEGER(0x04, 4, true),
    LONG(0x05, 8, true),
    FLAG(0x06, 1, true),
    FLOAT(0x07, 4, true),
    DOUBLE(0x08, 8, true),
    STRING(0x09, -1, true),
    UUID(0x0A, 16, true),
    NULL(0x0B, 0, true),
    VECTOR(0x80, -1, false),
    RESERVED(0xFF, -1, false);

    public final int code;
    /**
     * Size in bytes.
     * <p>
     * Size -1 means that value of that type has no fixed size.
     */
    public final int size;
    private final boolean primitive;

    Type(int code, int size, boolean isPrimitive) {
        this.code = code;
        this.size = size;
        this.primitive = isPrimitive;
    }

    /**
     * Returns {@code true} if {@link Type} is primitive.
     *
     * @return {@code true} if {@link Type} is primitive.
     */
    public boolean isPrimitive() {
        return primitive;
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
