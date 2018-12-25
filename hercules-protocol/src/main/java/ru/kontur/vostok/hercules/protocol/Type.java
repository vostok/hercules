package ru.kontur.vostok.hercules.protocol;

import java.util.Arrays;

/**
 * @author Gregory Koshelev
 */
public enum Type {
    CONTAINER(0x01),
    BYTE(0x02),
    SHORT(0x03),
    INTEGER(0x04),
    LONG(0x05),
    FLAG(0x06),
    FLOAT(0x07),
    DOUBLE(0x08),
    STRING(0x09),
    UUID(0x0A),
    NULL(0x0B),
    VECTOR(0x80),
    RESERVED(0xFF);

    public final int code;

    Type(int code) {
        this.code = code;
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
            CODES[(int)type.code] = type;
        }
    }
}
