package ru.kontur.vostok.hercules.protocol;


import org.junit.Assert;
import org.junit.Test;
import ru.kontur.vostok.hercules.protocol.decoder.Decoder;
import ru.kontur.vostok.hercules.protocol.encoder.Encoder;

import java.util.Arrays;

import static org.junit.Assert.*;

public class EncoderDecoderTest {

    @Test
    public void shouldEncodeDecodeByte() {
        Encoder encoder = new Encoder();
        encoder.writeByte((byte) 0xDE);

        Decoder decoder = new Decoder(encoder.getBytes());

        assertEquals((byte) 0xDE, decoder.readByte());
    }

    @Test
    public void shouldEncodeUnsignedByte() {
        Encoder encoder = new Encoder();
        encoder.writeUnsignedByte(0xDF);

        Decoder decoder = new Decoder(encoder.getBytes());

        assertEquals(0xDF, decoder.readUnsignedByte());
    }

    @Test
    public void shouldEncodeDecodeShort() {
        Encoder encoder = new Encoder();
        encoder.writeShort((short) 10_000);

        Decoder decoder = new Decoder(encoder.getBytes());

        assertEquals((short) 10_000, decoder.readShort());
    }

    @Test
    public void shouldEncodeDecodeInt() {
        Encoder encoder = new Encoder();
        encoder.writeInteger(0xDEADBEEF);

        Decoder decoder = new Decoder(encoder.getBytes());

        assertEquals(0xDEADBEEF, decoder.readInteger());
    }

    @Test
    public void shouldEncodeDecodeLong() {
        Encoder encoder = new Encoder();
        encoder.writeLong(0xDEADDEADBEEFBEEFL);

        Decoder decoder = new Decoder(encoder.getBytes());

        assertEquals(0xDEADDEADBEEFBEEFL, decoder.readLong());
    }

    @Test
    public void shouldEncodeDecodeFloat() {
        Encoder encoder = new Encoder();
        encoder.writeFloat(123.456f);

        Decoder decoder = new Decoder(encoder.getBytes());

        assertEquals(123.456f, decoder.readFloat(), 0);
    }

    @Test
    public void shouldEncodeDecodeDouble() {
        Encoder encoder = new Encoder();
        encoder.writeDouble(0.123456789);

        Decoder decoder = new Decoder(encoder.getBytes());

        assertEquals(0.123456789, decoder.readDouble(), 0);
    }

    @Test
    public void shouldEncodeDecodeFlag() {
        Encoder trueEncoder = new Encoder();
        trueEncoder.writeFlag(true);

        Encoder falseEncore = new Encoder();
        falseEncore.writeFlag(false);

        Decoder trueDecoder = new Decoder(trueEncoder.getBytes());
        Decoder falseDecoder = new Decoder(falseEncore.getBytes());

        assertTrue(trueDecoder.readFlag());
        assertFalse(falseDecoder.readFlag());
    }

    @Test
    public void shouldEncodeDecodeString() {
        Encoder encoder = new Encoder();
        encoder.writeString(
                "A sample string with UTF: АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧЩЪЫЬЭЮЯабвгдеёжхийклмнопрстуфхцчшщъыьэюя"
        );

        Decoder decoder = new Decoder(encoder.getBytes());

        assertEquals(
                "A sample string with UTF: АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧЩЪЫЬЭЮЯабвгдеёжхийклмнопрстуфхцчшщъыьэюя",
                decoder.readString()
        );
    }

    @Test
    public void shouldEncodeDecodeText() {
        Encoder encoder = new Encoder();
        encoder.writeText(multiply("Very very long text", 100));

        Decoder decoder = new Decoder(encoder.getBytes());

        assertEquals(multiply("Very very long text", 100), decoder.readText());
    }

    @Test
    public void shouldEncodeDecodeByteArray() {
        Encoder encoder = new Encoder();
        encoder.writeByteArray(multiply(new byte[]{(byte) 1, (byte) 2, (byte) 3}, 100));

        Decoder decoder = new Decoder(encoder.getBytes());

        assertArrayEquals(
                multiply(new byte[]{(byte) 1, (byte) 2, (byte) 3}, 100),
                decoder.readByteArray()
        );
    }

    @Test
    public void shouldEncodeDecodeByteVector() {
        Encoder encoder = new Encoder();
        encoder.writeByteVector(new byte[]{(byte) 1, (byte) 2, (byte) 3});

        Decoder decoder = new Decoder(encoder.getBytes());

        assertArrayEquals(new byte[]{(byte) 1, (byte) 2, (byte) 3}, decoder.readByteVector());
    }

    @Test
    public void shouldEncodeDecodeUnsignedByteArray() {
        Encoder encoder = new Encoder();
        encoder.writeUnsignedByteArray(multiply(new int[]{0, 100, 200}, 100));

        Decoder decoder = new Decoder(encoder.getBytes());

        assertArrayEquals(
                multiply(new int[]{0, 100, 200}, 100),
                decoder.readUnsignedByteArray()
        );
    }

    @Test
    public void shouldEncodeDecodeUnsignedByteVector() {
        Encoder encoder = new Encoder();
        encoder.writeUnsignedByteVector(new int[]{0, 100, 200});

        Decoder decoder = new Decoder(encoder.getBytes());

        assertArrayEquals(new int[]{0, 100, 200}, decoder.readUnsignedByteVector());
    }

    @Test
    public void shouldEncodeDecodeShortArray() {
        Encoder encoder = new Encoder();
        encoder.writeShortArray(multiply(new short[]{100, 10_000, 20_000}, 100));

        Decoder decoder = new Decoder(encoder.getBytes());

        assertArrayEquals(
                multiply(new short[]{100, 10_000, 20_000}, 100),
                decoder.readShortArray()
        );
    }

    @Test
    public void shouldEncodeDecodeShortVector() {
        Encoder encoder = new Encoder();
        encoder.writeShortVector(new short[]{100, 10_000, 20_000});

        Decoder decoder = new Decoder(encoder.getBytes());

        assertArrayEquals(new short[]{100, 10_000, 20_000}, decoder.readShortVector());
    }

    @Test
    public void shouldEncodeDecodeIntegerArray() {
        Encoder encoder = new Encoder();
        encoder.writeIntegerArray(multiply(new int[]{1_000, 10_000, 100_000}, 100));

        Decoder decoder = new Decoder(encoder.getBytes());

        assertArrayEquals(
                multiply(new int[]{1_000, 10_000, 100_000}, 100),
                decoder.readIntegerArray()
        );
    }

    @Test
    public void shouldEncodeDecodeIntegerVector() {
        Encoder encoder = new Encoder();
        encoder.writeIntegerVector(new int[]{1_000, 10_000, 100_000});

        Decoder decoder = new Decoder(encoder.getBytes());

        assertArrayEquals(new int[]{1_000, 10_000, 100_000}, decoder.readIntegerVector());
    }

    @Test
    public void shouldEncodeDecodeLongArray() {
        Encoder encoder = new Encoder();
        encoder.writeLongArray(multiply(new long[]{1_000, 10_000, 100_000}, 100));

        Decoder decoder = new Decoder(encoder.getBytes());

        assertArrayEquals(
                multiply(new long[]{1_000, 10_000, 100_000}, 100),
                decoder.readLongArray()
        );
    }

    @Test
    public void shouldEncodeDecodeLongVector() {
        Encoder encoder = new Encoder();
        encoder.writeLongVector(new long[]{1_000, 10_000, 100_000});

        Decoder decoder = new Decoder(encoder.getBytes());

        assertArrayEquals(new long[]{1_000, 10_000, 100_000}, decoder.readLongVector());
    }

    @Test
    public void shouldEncodeDecodeFlagArray() {
        Encoder encoder = new Encoder();
        encoder.writeFlagArray(multiply(new boolean[]{true, true, false}, 100));

        Decoder decoder = new Decoder(encoder.getBytes());

        assertArrayEquals(
                multiply(new boolean[]{true, true, false}, 100),
                decoder.readFlagArray()
        );
    }

    @Test
    public void shouldEncodeDecodeFlagVector() {
        Encoder encoder = new Encoder();
        encoder.writeFlagVector(new boolean[]{true, true, false});

        Decoder decoder = new Decoder(encoder.getBytes());

        assertArrayEquals(new boolean[]{true, true, false}, decoder.readFlagVector());
    }

    @Test
    public void shouldEncodeDecodeFloatArray() {
        Encoder encoder = new Encoder();
        encoder.writeFloatArray(multiply(new float[]{1.23f, 4.56f, 7.89f}, 100));

        Decoder decoder = new Decoder(encoder.getBytes());

        assertArrayEquals(
                multiply(new float[]{1.23f, 4.56f, 7.89f}, 100),
                decoder.readFloatArray(),
                0
        );
    }

    @Test
    public void shouldEncodeDecodeFloatVector() {
        Encoder encoder = new Encoder();
        encoder.writeFloatVector(new float[]{1.23f, 4.56f, 7.89f});

        Decoder decoder = new Decoder(encoder.getBytes());

        assertArrayEquals(new float[]{1.23f, 4.56f, 7.89f}, decoder.readFloatVector(), 0);
    }

    @Test
    public void shouldEncodeDecodeDoubleArray() {
        Encoder encoder = new Encoder();
        encoder.writeDoubleArray(multiply(new double[]{1.23, 4.56, 7.89}, 100));

        Decoder decoder = new Decoder(encoder.getBytes());

        assertArrayEquals(
                multiply(new double[]{1.23, 4.56, 7.89}, 100),
                decoder.readDoubleArray(),
                0
        );
    }

    @Test
    public void shouldEncodeDecodeDoubleVector() {
        Encoder encoder = new Encoder();
        encoder.writeDoubleVector(new double[]{1.23, 4.56, 7.89});

        Decoder decoder = new Decoder(encoder.getBytes());

        assertArrayEquals(new double[]{1.23, 4.56, 7.89}, decoder.readDoubleVector(), 0);
    }

    @Test
    public void shouldEncodeDecodeStringArray() {
        Encoder encoder = new Encoder();
        encoder.writeStringArray(multiply(new String[]{"a", "b", "c"}, 100));

        Decoder decoder = new Decoder(encoder.getBytes());

        assertArrayEquals(
                Arrays.stream(multiply(new String[]{"a", "b", "c"}, 100)).map(String::getBytes).toArray(),
                decoder.readStringArrayAsBytes()
        );
    }

    @Test
    public void shouldEncodeDecodeStringVector() {
        Encoder encoder = new Encoder();
        encoder.writeStringVector(new String[]{"a", "b", "c"});

        Decoder decoder = new Decoder(encoder.getBytes());

        assertArrayEquals(
                Arrays.stream(new String[]{"a", "b", "c"}).map(String::getBytes).toArray(),
                decoder.readStringVectorAsBytes()
        );
    }

    @Test
    public void shouldEncodeDecodeTextArray() {
        Encoder encoder = new Encoder();
        encoder.writeTextArray(multiply(new String[]{multiply("abc", 100)}, 300));

        Decoder decoder = new Decoder(encoder.getBytes());

        assertArrayEquals(
                Arrays.stream(multiply(new String[]{multiply("abc", 100)}, 300)).map(String::getBytes).toArray(),
                decoder.readTextArrayAsBytes()
        );
    }

    @Test
    public void shouldEncodeDecodeTextVector() {
        Encoder encoder = new Encoder();
        encoder.writeTextVector(new String[]{multiply("abc", 100)});

        Decoder decoder = new Decoder(encoder.getBytes());

        assertArrayEquals(
                Arrays.stream(new String[]{multiply("abc", 100)}).map(String::getBytes).toArray(),
                decoder.readTextVectorAsBytes()
        );
    }

    private static String multiply(String s, int count) {
        StringBuilder res = new StringBuilder(s.length() * count);
        for (int i = 0; i < count; ++i) {
            res.append(s);
        }
        return res.toString();
    }

    private static byte[] multiply(byte[] array, int count) {
        byte[] result = new byte[array.length * count];
        for (int i = 0; i < result.length; i += array.length) {
            System.arraycopy(array, 0, result, i, array.length);
        }
        return result;
    }

    private static int[] multiply(int[] array, int count) {
        int[] result = new int[array.length * count];
        for (int i = 0; i < result.length; i += array.length) {
            System.arraycopy(array, 0, result, i, array.length);
        }
        return result;
    }

    private static short[] multiply(short[] array, int count) {
        short[] result = new short[array.length * count];
        for (int i = 0; i < result.length; i += array.length) {
            System.arraycopy(array, 0, result, i, array.length);
        }
        return result;
    }

    private static long[] multiply(long[] array, int count) {
        long[] result = new long[array.length * count];
        for (int i = 0; i < result.length; i += array.length) {
            System.arraycopy(array, 0, result, i, array.length);
        }
        return result;
    }

    private static boolean[] multiply(boolean[] array, int count) {
        boolean[] result = new boolean[array.length * count];
        for (int i = 0; i < result.length; i += array.length) {
            System.arraycopy(array, 0, result, i, array.length);
        }
        return result;
    }

    private static float[] multiply(float[] array, int count) {
        float[] result = new float[array.length * count];
        for (int i = 0; i < result.length; i += array.length) {
            System.arraycopy(array, 0, result, i, array.length);
        }
        return result;
    }

    private static double[] multiply(double[] array, int count) {
        double[] result = new double[array.length * count];
        for (int i = 0; i < result.length; i += array.length) {
            System.arraycopy(array, 0, result, i, array.length);
        }
        return result;
    }

    private static String[] multiply(String[] array, int count) {
        String[] result = new String[array.length * count];
        for (int i = 0; i < result.length; i += array.length) {
            System.arraycopy(array, 0, result, i, array.length);
        }
        return result;
    }
}
