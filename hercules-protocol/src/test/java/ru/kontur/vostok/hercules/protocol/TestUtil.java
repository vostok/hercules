package ru.kontur.vostok.hercules.protocol;

import ru.kontur.vostok.hercules.protocol.util.EventBuilder;
import ru.kontur.vostok.hercules.util.time.TimeUtil;
import ru.kontur.vostok.hercules.uuid.UuidGenerator;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class TestUtil {

    static String multiply(String s, int count) {
        StringBuilder res = new StringBuilder(s.length() * count);
        for (int i = 0; i < count; ++i) {
            res.append(s);
        }
        return res.toString();
    }

    static Container[] multiply(Container[] array, int count) {
        Container[] result = new Container[array.length * count];
        for (int i = 0; i < result.length; i += array.length) {
            System.arraycopy(array, 0, result, i, array.length);
        }

        return result;
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

    static Event createEvent() {
        return EventBuilder.create(
            TimeUtil.millisToTicks(123_456_789L),
                UuidGenerator.getClientInstance().withTicks(TimeUtil.unixMillisToGregorianTicks(123_456_789L))
        )
                .tag("string", Variant.ofString("Abc ЕЁЮ"))
                .tag("int", Variant.ofInteger(123))
                .build();
    }

    public static String getResourceAsString(final String resource) {
        try (InputStream stream = TestUtil.class.getClassLoader().getResourceAsStream(resource)) {
            final ByteArrayOutputStream result = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int length;
            while ((length = stream.read(buffer)) != -1) {
                result.write(buffer, 0, length);
            }
            return result.toString(StandardCharsets.UTF_8.name());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private TestUtil() {
        /* static class */
    }
}
