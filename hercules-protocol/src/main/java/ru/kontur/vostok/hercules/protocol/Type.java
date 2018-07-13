package ru.kontur.vostok.hercules.protocol;

import java.util.Arrays;

/**
 * @author Gregory Koshelev
 */
public enum Type {
    CONTAINER((byte) 0x00),
    BYTE((byte)0x01),
    SHORT((byte)0x02),
    INTEGER((byte)0x03),
    LONG((byte)0x04),
    FLAG((byte)0x05),
    FLOAT((byte)0x06),
    DOUBLE((byte)0x07),
    STRING((byte)0x08),
    TEXT((byte)0x09),
    CONTAINER_ARRAY((byte) 0x0A),
    BYTE_ARRAY((byte)0x0B),
    SHORT_ARRAY((byte)0x0C),
    INTEGER_ARRAY((byte)0x0D),
    LONG_ARRAY((byte)0x0E),
    FLAG_ARRAY((byte)0x0F),
    FLOAT_ARRAY((byte)0x10),
    DOUBLE_ARRAY((byte)0x11),
    STRING_ARRAY((byte)0x12),
    TEXT_ARRAY((byte)0x13),
    CONTAINER_VECTOR((byte) 0x14),
    BYTE_VECTOR((byte)0x15),
    SHORT_VECTOR((byte)0x16),
    INTEGER_VECTOR((byte)0x17),
    LONG_VECTOR((byte)0x18),
    FLAG_VECTOR((byte)0x19),
    FLOAT_VECTOR((byte)0x1A),
    DOUBLE_VECTOR((byte)0x1B),
    STRING_VECTOR((byte)0x1C),
    TEXT_VECTOR((byte)0x1D),
    RESERVED((byte)0xFF);

    public final byte code;

    Type(byte code) {
        this.code = code;
    }

    public static Type valueOf(byte code) {
        return codes[code & 0xFF];
    }

    private final static Type[] codes = new Type[256];
    static {
        Arrays.fill(codes, RESERVED);

        for (Type type : values()) {
            if (RESERVED == type) {
                continue; // Skip RESERVED cause it has special code
            }
            codes[type.code] = type;
        }
    }
}
