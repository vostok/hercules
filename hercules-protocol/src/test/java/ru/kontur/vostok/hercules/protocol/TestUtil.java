package ru.kontur.vostok.hercules.protocol;

import java.nio.charset.StandardCharsets;

public final class TestUtil {

    static String multiply(String s, int count) {
        StringBuilder res = new StringBuilder(s.length() * count);
        for (int i = 0; i < count; ++i) {
            res.append(s);
        }
        return res.toString();
    }

    static byte[] multiply(byte[] array, int count) {
        byte[] result = new byte[array.length * count];
        for (int i = 0; i < result.length; i += array.length) {
            System.arraycopy(array, 0, result, i, array.length);
        }
        return result;
    }

    static int[] multiply(int[] array, int count) {
        int[] result = new int[array.length * count];
        for (int i = 0; i < result.length; i += array.length) {
            System.arraycopy(array, 0, result, i, array.length);
        }
        return result;
    }

    static short[] multiply(short[] array, int count) {
        short[] result = new short[array.length * count];
        for (int i = 0; i < result.length; i += array.length) {
            System.arraycopy(array, 0, result, i, array.length);
        }
        return result;
    }

    static long[] multiply(long[] array, int count) {
        long[] result = new long[array.length * count];
        for (int i = 0; i < result.length; i += array.length) {
            System.arraycopy(array, 0, result, i, array.length);
        }
        return result;
    }

    static boolean[] multiply(boolean[] array, int count) {
        boolean[] result = new boolean[array.length * count];
        for (int i = 0; i < result.length; i += array.length) {
            System.arraycopy(array, 0, result, i, array.length);
        }
        return result;
    }

    static float[] multiply(float[] array, int count) {
        float[] result = new float[array.length * count];
        for (int i = 0; i < result.length; i += array.length) {
            System.arraycopy(array, 0, result, i, array.length);
        }
        return result;
    }

    static double[] multiply(double[] array, int count) {
        double[] result = new double[array.length * count];
        for (int i = 0; i < result.length; i += array.length) {
            System.arraycopy(array, 0, result, i, array.length);
        }
        return result;
    }

    static String[] multiply(String[] array, int count) {
        String[] result = new String[array.length * count];
        for (int i = 0; i < result.length; i += array.length) {
            System.arraycopy(array, 0, result, i, array.length);
        }
        return result;
    }

    static byte[][] toBytes(String[] strings) {
        byte[][] result = new byte[strings.length][];
        for (int i = 0; i < strings.length; ++i) {
            result[i] = strings[i].getBytes(StandardCharsets.UTF_8);
        }
        return result;
    }

    private TestUtil() {
    }
}
