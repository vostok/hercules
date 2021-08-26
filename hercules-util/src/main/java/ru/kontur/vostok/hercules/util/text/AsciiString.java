package ru.kontur.vostok.hercules.util.text;

import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * @author Gregory Koshelev
 */
public class AsciiString implements CharSequence {
    private final byte[] data;

    public AsciiString(byte[] data) {
        this.data = data;
    }

    @Override
    public int length() {
        return data.length;
    }

    @Override
    public char charAt(int index) {
        return (char) data[index];
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return new AsciiString(Arrays.copyOfRange(data, start, end));
    }

    @Override
    @NotNull
    public String toString() {
        return new String(data, StandardCharsets.US_ASCII);
    }
}
