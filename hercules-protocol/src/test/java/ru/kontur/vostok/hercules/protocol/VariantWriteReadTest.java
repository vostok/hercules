package ru.kontur.vostok.hercules.protocol;

import org.junit.Test;
import ru.kontur.vostok.hercules.protocol.decoder.Decoder;
import ru.kontur.vostok.hercules.protocol.decoder.VariantReader;
import ru.kontur.vostok.hercules.protocol.encoder.Encoder;
import ru.kontur.vostok.hercules.protocol.encoder.VariantWriter;

import static java.nio.charset.StandardCharsets.*;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static ru.kontur.vostok.hercules.protocol.TestUtil.multiply;
import static ru.kontur.vostok.hercules.protocol.TestUtil.toBytes;

public class VariantWriteReadTest {

    @Test
    public void shouldWriteReadByte() {
        Variant variant = Variant.ofByte((byte) 127);

        Encoder encoder = new Encoder();
        VariantWriter.write(encoder, variant);

        Decoder decoder = new Decoder(encoder.getBytes());
        Variant result = VariantReader.read(decoder);

        assertEquals(Type.BYTE, result.getType());
        assertEquals((byte) 127, (byte) result.getValue());
    }

    @Test
    public void shouldWriteReadShort() {
        Variant variant = Variant.ofShort((short) 10_000);

        Encoder encoder = new Encoder();
        VariantWriter.write(encoder, variant);

        Decoder decoder = new Decoder(encoder.getBytes());
        Variant result = VariantReader.read(decoder);

        assertEquals(Type.SHORT, result.getType());
        assertEquals((short) 10_000, (short) result.getValue());
    }

    @Test
    public void shouldWriteReadInteger() {
        Variant variant = Variant.ofInteger(123_456_789);

        Encoder encoder = new Encoder();
        VariantWriter.write(encoder, variant);

        Decoder decoder = new Decoder(encoder.getBytes());
        Variant result = VariantReader.read(decoder);

        assertEquals(Type.INTEGER, result.getType());
        assertEquals((int) 123_456_789, (int) result.getValue());
    }

    @Test
    public void shouldWriteReadLong() {
        Variant variant = Variant.ofLong(123_456_789L);

        Encoder encoder = new Encoder();
        VariantWriter.write(encoder, variant);

        Decoder decoder = new Decoder(encoder.getBytes());
        Variant result = VariantReader.read(decoder);

        assertEquals(Type.LONG, result.getType());
        assertEquals((long) 123_456_789L, (long) result.getValue());
    }

    @Test
    public void shouldWriteReadFloat() {
        Variant variant = Variant.ofFloat(0.123456f);

        Encoder encoder = new Encoder();
        VariantWriter.write(encoder, variant);

        Decoder decoder = new Decoder(encoder.getBytes());
        Variant result = VariantReader.read(decoder);

        assertEquals(Type.FLOAT, result.getType());
        assertEquals((float) 0.123456f, (float) result.getValue(), 0);
    }

    @Test
    public void shouldWriteReadDouble() {
        Variant variant = Variant.ofDouble(0.123456789);

        Encoder encoder = new Encoder();
        VariantWriter.write(encoder, variant);

        Decoder decoder = new Decoder(encoder.getBytes());
        Variant result = VariantReader.read(decoder);

        assertEquals(Type.DOUBLE, result.getType());
        assertEquals(0.123456789, (double) result.getValue(), 0);
    }

    @Test
    public void shouldWriteReadFlag() {
        Variant variant = Variant.ofFlag(true);

        Encoder encoder = new Encoder();
        VariantWriter.write(encoder, variant);

        Decoder decoder = new Decoder(encoder.getBytes());
        Variant result = VariantReader.read(decoder);

        assertEquals(Type.FLAG, result.getType());
        assertTrue((boolean) result.getValue());
    }

    @Test
    public void shouldWriteReadString() {
        Variant variant = Variant.ofString("Abc def Абв гдё");

        Encoder encoder = new Encoder();
        VariantWriter.write(encoder, variant);

        Decoder decoder = new Decoder(encoder.getBytes());
        Variant result = VariantReader.read(decoder);

        assertEquals(Type.STRING, result.getType());
        assertArrayEquals("Abc def Абв гдё".getBytes(UTF_8), (byte[]) result.getValue());
    }

    @Test
    public void shouldWriteReadText() {
        Variant variant = Variant.ofText("Abc def Абв гдё");

        Encoder encoder = new Encoder();
        VariantWriter.write(encoder, variant);

        Decoder decoder = new Decoder(encoder.getBytes());
        Variant result = VariantReader.read(decoder);

        assertEquals(Type.TEXT, result.getType());
        assertArrayEquals("Abc def Абв гдё".getBytes(UTF_8), (byte[]) result.getValue());
    }

    @Test
    public void shouldWriteReadByteVector() {
        Variant variant = Variant.ofByteVector(new byte[]{1, 2, 3});

        Encoder encoder = new Encoder();
        VariantWriter.write(encoder, variant);

        Decoder decoder = new Decoder(encoder.getBytes());
        Variant result = VariantReader.read(decoder);

        assertEquals(Type.BYTE_VECTOR, result.getType());
        assertArrayEquals(new byte[]{1, 2, 3}, (byte[]) result.getValue());
    }

    @Test
    public void shouldWriteReadShortVector() {
        Variant variant = Variant.ofShortVector(new short[]{10_000, 20_000, 30_000});

        Encoder encoder = new Encoder();
        VariantWriter.write(encoder, variant);

        Decoder decoder = new Decoder(encoder.getBytes());
        Variant result = VariantReader.read(decoder);

        assertEquals(Type.SHORT_VECTOR, result.getType());
        assertArrayEquals(new short[]{10_000, 20_000, 30_000}, (short[]) result.getValue());
    }

    @Test
    public void shouldWriteReadIntegerVector() {
        Variant variant = Variant.ofIntegerVector(new int[]{1, 2, 123_456_789});

        Encoder encoder = new Encoder();
        VariantWriter.write(encoder, variant);

        Decoder decoder = new Decoder(encoder.getBytes());
        Variant result = VariantReader.read(decoder);

        assertEquals(Type.INTEGER_VECTOR, result.getType());
        assertArrayEquals(new int[]{1, 2, 123_456_789}, (int[]) result.getValue());
    }

    @Test
    public void shouldWriteReadLongVector() {
        Variant variant = Variant.ofLongVector(new long[] {1, 2, 123_456_789L});

        Encoder encoder = new Encoder();
        VariantWriter.write(encoder, variant);

        Decoder decoder = new Decoder(encoder.getBytes());
        Variant result = VariantReader.read(decoder);

        assertEquals(Type.LONG_VECTOR, result.getType());
        assertArrayEquals(new long[]{1, 2, 123_456_789L}, (long[]) result.getValue());
    }

    @Test
    public void shouldWriteReadFloatVector() {
        Variant variant = Variant.ofFloatVector(new float[] {1.23f, 4.56f, 7.89f});

        Encoder encoder = new Encoder();
        VariantWriter.write(encoder, variant);

        Decoder decoder = new Decoder(encoder.getBytes());
        Variant result = VariantReader.read(decoder);

        assertEquals(Type.FLOAT_VECTOR, result.getType());
        assertArrayEquals(new float[]{1.23f, 4.56f, 7.89f}, (float[]) result.getValue(), 0);
    }

    @Test
    public void shouldWriteReadDoubleVector() {
        Variant variant = Variant.ofDoubleVector(new double[]{1.23, 4.56, 7.89});

        Encoder encoder = new Encoder();
        VariantWriter.write(encoder, variant);

        Decoder decoder = new Decoder(encoder.getBytes());
        Variant result = VariantReader.read(decoder);

        assertEquals(Type.DOUBLE_VECTOR, result.getType());
        assertArrayEquals(new double[]{1.23, 4.56, 7.89}, (double[]) result.getValue(), 0);
    }

    @Test
    public void shouldWriteReadFlagVector() {
        Variant variant = Variant.ofFlagVector(new boolean[] {true, true, false});

        Encoder encoder = new Encoder();
        VariantWriter.write(encoder, variant);

        Decoder decoder = new Decoder(encoder.getBytes());
        Variant result = VariantReader.read(decoder);

        assertEquals(Type.FLAG_VECTOR, result.getType());
        assertArrayEquals(new boolean[] {true, true, false}, (boolean[]) result.getValue());
    }

    @Test
    public void shouldWriteReadStringVector() {
        Variant variant = Variant.ofStringVector(new String[]{"S", "F", "Ё"});

        Encoder encoder = new Encoder();
        VariantWriter.write(encoder, variant);

        Decoder decoder = new Decoder(encoder.getBytes());
        Variant result = VariantReader.read(decoder);

        assertEquals(Type.STRING_VECTOR, result.getType());
        assertArrayEquals(toBytes(new String[]{"S", "F", "Ё"}), (byte[][]) result.getValue());
    }


    @Test
    public void shouldWriteReadTextVector() {
        Variant variant = Variant.ofTextVector(new String[]{"S", "F", multiply("ЁЙЯ", 100)});

        Encoder encoder = new Encoder();
        VariantWriter.write(encoder, variant);

        Decoder decoder = new Decoder(encoder.getBytes());
        Variant result = VariantReader.read(decoder);

        assertEquals(Type.TEXT_VECTOR, result.getType());
        assertArrayEquals(toBytes(new String[]{"S", "F", multiply("ЁЙЯ", 100)}), (byte[][]) result.getValue());
    }

    @Test
    public void shouldWriteReadByteArray() {
        Variant variant = Variant.ofByteArray(multiply(new byte[]{1, 2, 3}, 100));

        Encoder encoder = new Encoder();
        VariantWriter.write(encoder, variant);

        Decoder decoder = new Decoder(encoder.getBytes());
        Variant result = VariantReader.read(decoder);

        assertEquals(Type.BYTE_ARRAY, result.getType());
        assertArrayEquals(multiply(new byte[]{1, 2, 3}, 100), (byte[]) result.getValue());
    }

    @Test
    public void shouldWriteReadShortArray() {
        Variant variant = Variant.ofShortArray(multiply(new short[]{10_000, 20_000, 30_000}, 100));

        Encoder encoder = new Encoder();
        VariantWriter.write(encoder, variant);

        Decoder decoder = new Decoder(encoder.getBytes());
        Variant result = VariantReader.read(decoder);

        assertEquals(Type.SHORT_ARRAY, result.getType());
        assertArrayEquals(multiply(new short[]{10_000, 20_000, 30_000}, 100), (short[]) result.getValue());
    }

    @Test
    public void shouldWriteReadIntegerArray() {
        Variant variant = Variant.ofIntegerArray(multiply(new int[]{1, 2, 123_456_789}, 100));

        Encoder encoder = new Encoder();
        VariantWriter.write(encoder, variant);

        Decoder decoder = new Decoder(encoder.getBytes());
        Variant result = VariantReader.read(decoder);

        assertEquals(Type.INTEGER_ARRAY, result.getType());
        assertArrayEquals(multiply(new int[]{1, 2, 123_456_789}, 100), (int[]) result.getValue());
    }

    @Test
    public void shouldWriteReadLongArray() {
        Variant variant = Variant.ofLongArray(multiply(new long[] {1, 2, 123_456_789L}, 100));

        Encoder encoder = new Encoder();
        VariantWriter.write(encoder, variant);

        Decoder decoder = new Decoder(encoder.getBytes());
        Variant result = VariantReader.read(decoder);

        assertEquals(Type.LONG_ARRAY, result.getType());
        assertArrayEquals(multiply(new long[]{1, 2, 123_456_789L}, 100), (long[]) result.getValue());
    }

    @Test
    public void shouldWriteReadFloatArray() {
        Variant variant = Variant.ofFloatArray(multiply(new float[] {1.23f, 4.56f, 7.89f}, 100));

        Encoder encoder = new Encoder();
        VariantWriter.write(encoder, variant);

        Decoder decoder = new Decoder(encoder.getBytes());
        Variant result = VariantReader.read(decoder);

        assertEquals(Type.FLOAT_ARRAY, result.getType());
        assertArrayEquals(multiply(new float[]{1.23f, 4.56f, 7.89f}, 100), (float[]) result.getValue(), 0);
    }

    @Test
    public void shouldWriteReadDoubleArray() {
        Variant variant = Variant.ofDoubleArray(multiply(new double[]{1.23, 4.56, 7.89}, 100));

        Encoder encoder = new Encoder();
        VariantWriter.write(encoder, variant);

        Decoder decoder = new Decoder(encoder.getBytes());
        Variant result = VariantReader.read(decoder);

        assertEquals(Type.DOUBLE_ARRAY, result.getType());
        assertArrayEquals(multiply(new double[]{1.23, 4.56, 7.89}, 100), (double[]) result.getValue(), 0);
    }

    @Test
    public void shouldWriteReadFlagArray() {
        Variant variant = Variant.ofFlagArray(multiply(new boolean[] {true, true, false}, 100));

        Encoder encoder = new Encoder();
        VariantWriter.write(encoder, variant);

        Decoder decoder = new Decoder(encoder.getBytes());
        Variant result = VariantReader.read(decoder);

        assertEquals(Type.FLAG_ARRAY, result.getType());
        assertArrayEquals(multiply(new boolean[] {true, true, false}, 100), (boolean[]) result.getValue());
    }

    @Test
    public void shouldWriteReadStringArray() {
        Variant variant = Variant.ofStringArray(multiply(new String[]{"S", "F", "Ё"}, 100));

        Encoder encoder = new Encoder();
        VariantWriter.write(encoder, variant);

        Decoder decoder = new Decoder(encoder.getBytes());
        Variant result = VariantReader.read(decoder);

        assertEquals(Type.STRING_ARRAY, result.getType());
        assertArrayEquals(toBytes(multiply(new String[]{"S", "F", "Ё"}, 100)), (byte[][]) result.getValue());
    }


    @Test
    public void shouldWriteReadTextArray() {
        Variant variant = Variant.ofTextArray(multiply(new String[]{"S", "F", multiply("ЁЙЯ", 99)}, 100));

        Encoder encoder = new Encoder();
        VariantWriter.write(encoder, variant);

        Decoder decoder = new Decoder(encoder.getBytes());
        Variant result = VariantReader.read(decoder);

        assertEquals(Type.TEXT_ARRAY, result.getType());
        assertArrayEquals(toBytes(multiply(new String[]{"S", "F", multiply("ЁЙЯ", 99)}, 100)), (byte[][]) result.getValue());
    }
}
