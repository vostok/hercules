package ru.kontur.vostok.hercules.protocol;

import ru.kontur.vostok.hercules.util.arguments.Preconditions;

import java.nio.charset.StandardCharsets;

/**
 * @author Gregory Koshelev
 */
public class TinyString implements CharSequence {
    private static final int SIZE_OF_TINY_STRING_LENGTH = Type.BYTE.size;

    private static final TinyString EMPTY = new TinyString(new byte[0]);

    private final byte[] bytes;
    private int hash;

    private TinyString(byte[] bytes) {
        this.bytes = bytes;
    }

    public int length() {
        return bytes.length;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public int sizeOf() {
        return SIZE_OF_TINY_STRING_LENGTH + length();
    }

    @Override
    public int hashCode() {
        int h = hash;
        if (h == 0 && bytes.length > 0) {
            for (int i = 0; i < bytes.length; i++) {
                h = 31 * h + bytes[i];
            }
            hash = h;
        }
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof TinyString) {
            TinyString another = (TinyString) obj;
            if (bytes.length == another.bytes.length) {
                int i = 0;
                while (i < bytes.length) {
                    if (bytes[i] != another.bytes[i]) {
                        return false;
                    }
                    i++;
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return new String(bytes, StandardCharsets.UTF_8);
    }

    @Override
    public char charAt(int index) {
        return (char) bytes[index];
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        Preconditions.check(start < bytes.length, "start must be less than string length");
        Preconditions.check(end <= bytes.length, "end must be less or equal to string length");
        Preconditions.check(start <= end, "start must be be less or equal to end");
        return new CharSequence() {
            @Override
            public int length() {
                return end - start;
            }

            @Override
            public char charAt(int index) {
                Preconditions.check(index < length());
                return (char) bytes[start + index];
            }

            @Override
            public CharSequence subSequence(int startInner, int endInner) {
                return TinyString.this.subSequence(start + startInner, start + endInner);
            }
        };
    }

    public static TinyString of(String s) {
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        return new TinyString(bytes);
    }

    public static TinyString of(byte[] bytes) {
        return new TinyString(bytes);
    }

    public static TinyString empty() {
        return EMPTY;
    }

    public static TinyString[] toTinyStrings(String... strings) {
        TinyString[] tinyStrings = new TinyString[strings.length];
        for (int i = 0; i < strings.length; i++) {
            tinyStrings[i] = TinyString.of(strings[i]);
        }
        return tinyStrings;
    }
}
