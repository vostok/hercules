package ru.kontur.vostok.hercules.protocol;

import java.nio.charset.StandardCharsets;

/**
 * @author Gregory Koshelev
 */
public class TinyString {
    private static final TinyString EMPTY = new TinyString(new byte[0]);

    private final byte[] bytes;
    private int hash;

    private TinyString(byte[] bytes) {
        this.bytes = bytes;
    }

    public int size() {
        return bytes.length;
    }

    public byte[] getBytes() {
        return bytes;
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
