package ru.kontur.vostok.hercules.protocol.util;

import ru.kontur.vostok.hercules.protocol.Container;

import java.nio.charset.StandardCharsets;

/**
 * StandardExtractors
 *
 * @author Kirill Sulim
 */
public final class StandardExtractors {

    public static String extractString(Object byteArrayInUtf8) {
        return new String((byte[]) byteArrayInUtf8, StandardCharsets.UTF_8);
    }

    public static String[] extractStringArray(Object byteArrayInUtf8Array) {
        byte[][] value = (byte[][]) byteArrayInUtf8Array;
        String[] result = new String[value.length];
        for (int i = 0; i < value.length; ++i) {
            result[i] = new String(value[i], StandardCharsets.UTF_8);
        }
        return result;
    }

    public static Double extractDouble(Object doubleObject) {
        return (Double) doubleObject;
    }

    public static Container[] extractContainerArray(Object containerList) {
        return (Container[]) containerList;
    }

    public static Container extractContainer(final Object o) {
        return (Container) o;
    }

    private StandardExtractors() {
        /* static class */
    }
}
