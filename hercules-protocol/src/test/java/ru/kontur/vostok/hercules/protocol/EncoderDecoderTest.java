package ru.kontur.vostok.hercules.protocol;


import org.junit.Test;
import ru.kontur.vostok.hercules.protocol.decoder.Decoder;
import ru.kontur.vostok.hercules.protocol.encoder.Encoder;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class EncoderDecoderTest {

    @Test
    public void shouldEncodeDecodeByte() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        Encoder encoder = new Encoder(stream);
        encoder.writeByte((byte) 0xDE);

        Decoder decoder = new Decoder(stream.toByteArray());

        assertEquals((byte) 0xDE, decoder.readByte());
    }

    @Test
    public void shouldEncodeUnsignedByte() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        Encoder encoder = new Encoder(stream);
        encoder.writeUnsignedByte(0xDF);

        Decoder decoder = new Decoder(stream.toByteArray());

        assertEquals(0xDF, decoder.readUnsignedByte());
    }

    @Test
    public void shouldEncodeDecodeShort() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        Encoder encoder = new Encoder(stream);
        encoder.writeShort((short) 10_000);

        Decoder decoder = new Decoder(stream.toByteArray());

        assertEquals((short) 10_000, decoder.readShort());
    }

    @Test
    public void shouldEncodeDecodeInt() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        Encoder encoder = new Encoder(stream);
        encoder.writeInteger(0xDEADBEEF);

        Decoder decoder = new Decoder(stream.toByteArray());

        assertEquals(0xDEADBEEF, decoder.readInteger());
    }

    @Test
    public void shouldEncodeDecodeLong() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        Encoder encoder = new Encoder(stream);
        encoder.writeLong(0xDEADDEADBEEFBEEFL);

        Decoder decoder = new Decoder(stream.toByteArray());

        assertEquals(0xDEADDEADBEEFBEEFL, decoder.readLong());
    }

    @Test
    public void shouldEncodeDecodeFloat() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        Encoder encoder = new Encoder(stream);
        encoder.writeFloat(123.456f);

        Decoder decoder = new Decoder(stream.toByteArray());

        assertEquals(123.456f, decoder.readFloat(), 0);
    }

    @Test
    public void shouldEncodeDecodeDouble() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        Encoder encoder = new Encoder(stream);
        encoder.writeDouble(0.123456789);

        Decoder decoder = new Decoder(stream.toByteArray());

        assertEquals(0.123456789, decoder.readDouble(), 0);
    }

    @Test
    public void shouldEncodeDecodeFlag() {
        ByteArrayOutputStream trueStream = new ByteArrayOutputStream();
        Encoder trueEncoder = new Encoder(trueStream);
        trueEncoder.writeFlag(true);

        ByteArrayOutputStream falseStream = new ByteArrayOutputStream();
        Encoder falseEncore = new Encoder(falseStream);
        falseEncore.writeFlag(false);

        Decoder trueDecoder = new Decoder(trueStream.toByteArray());
        Decoder falseDecoder = new Decoder(falseStream.toByteArray());

        assertTrue(trueDecoder.readFlag());
        assertFalse(falseDecoder.readFlag());
    }

    @Test
    public void shouldEncodeDecodeString() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        Encoder encoder = new Encoder(stream);
        encoder.writeString(
                "A sample string with UTF: АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧЩЪЫЬЭЮЯабвгдеёжхийклмнопрстуфхцчшщъыьэюя"
        );

        Decoder decoder = new Decoder(stream.toByteArray());

        assertEquals(
                "A sample string with UTF: АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧЩЪЫЬЭЮЯабвгдеёжхийклмнопрстуфхцчшщъыьэюя",
                decoder.readString()
        );
    }

    @Test
    public void shouldEncodeDecodeText() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        Encoder encoder = new Encoder(stream);
        encoder.writeText(TestUtil.multiply("Very very long text", 100));

        Decoder decoder = new Decoder(stream.toByteArray());

        assertEquals(TestUtil.multiply("Very very long text", 100), decoder.readText());
    }

    @Test
    public void shouldEncodeDecodeByteArray() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        Encoder encoder = new Encoder(stream);
        encoder.writeByteArray(TestUtil.multiply(new byte[]{(byte) 1, (byte) 2, (byte) 3}, 100));

        Decoder decoder = new Decoder(stream.toByteArray());

        assertArrayEquals(
                TestUtil.multiply(new byte[]{(byte) 1, (byte) 2, (byte) 3}, 100),
                decoder.readByteArray()
        );
    }

    @Test
    public void shouldEncodeDecodeByteVector() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        Encoder encoder = new Encoder(stream);
        encoder.writeByteVector(new byte[]{(byte) 1, (byte) 2, (byte) 3});

        Decoder decoder = new Decoder(stream.toByteArray());

        assertArrayEquals(new byte[]{(byte) 1, (byte) 2, (byte) 3}, decoder.readByteVector());
    }

    @Test
    public void shouldEncodeDecodeUnsignedByteArray() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        Encoder encoder = new Encoder(stream);
        encoder.writeUnsignedByteArray(TestUtil.multiply(new int[]{0, 100, 200}, 100));

        Decoder decoder = new Decoder(stream.toByteArray());

        assertArrayEquals(
                TestUtil.multiply(new int[]{0, 100, 200}, 100),
                decoder.readUnsignedByteArray()
        );
    }

    @Test
    public void shouldEncodeDecodeUnsignedByteVector() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        Encoder encoder = new Encoder(stream);
        encoder.writeUnsignedByteVector(new int[]{0, 100, 200});

        Decoder decoder = new Decoder(stream.toByteArray());

        assertArrayEquals(new int[]{0, 100, 200}, decoder.readUnsignedByteVector());
    }

    @Test
    public void shouldEncodeDecodeShortArray() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        Encoder encoder = new Encoder(stream);
        encoder.writeShortArray(TestUtil.multiply(new short[]{100, 10_000, 20_000}, 100));

        Decoder decoder = new Decoder(stream.toByteArray());

        assertArrayEquals(
                TestUtil.multiply(new short[]{100, 10_000, 20_000}, 100),
                decoder.readShortArray()
        );
    }

    @Test
    public void shouldEncodeDecodeShortVector() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        Encoder encoder = new Encoder(stream);
        encoder.writeShortVector(new short[]{100, 10_000, 20_000});

        Decoder decoder = new Decoder(stream.toByteArray());

        assertArrayEquals(new short[]{100, 10_000, 20_000}, decoder.readShortVector());
    }

    @Test
    public void shouldEncodeDecodeIntegerArray() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        Encoder encoder = new Encoder(stream);
        encoder.writeIntegerArray(TestUtil.multiply(new int[]{1_000, 10_000, 100_000}, 100));

        Decoder decoder = new Decoder(stream.toByteArray());

        assertArrayEquals(
                TestUtil.multiply(new int[]{1_000, 10_000, 100_000}, 100),
                decoder.readIntegerArray()
        );
    }

    @Test
    public void shouldEncodeDecodeIntegerVector() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        Encoder encoder = new Encoder(stream);
        encoder.writeIntegerVector(new int[]{1_000, 10_000, 100_000});

        Decoder decoder = new Decoder(stream.toByteArray());

        assertArrayEquals(new int[]{1_000, 10_000, 100_000}, decoder.readIntegerVector());
    }

    @Test
    public void shouldEncodeDecodeLongArray() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        Encoder encoder = new Encoder(stream);
        encoder.writeLongArray(TestUtil.multiply(new long[]{1_000, 10_000, 100_000}, 100));

        Decoder decoder = new Decoder(stream.toByteArray());

        assertArrayEquals(
                TestUtil.multiply(new long[]{1_000, 10_000, 100_000}, 100),
                decoder.readLongArray()
        );
    }

    @Test
    public void shouldEncodeDecodeLongVector() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        Encoder encoder = new Encoder(stream);
        encoder.writeLongVector(new long[]{1_000, 10_000, 100_000});

        Decoder decoder = new Decoder(stream.toByteArray());

        assertArrayEquals(new long[]{1_000, 10_000, 100_000}, decoder.readLongVector());
    }

    @Test
    public void shouldEncodeDecodeFlagArray() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        Encoder encoder = new Encoder(stream);
        encoder.writeFlagArray(TestUtil.multiply(new boolean[]{true, true, false}, 100));

        Decoder decoder = new Decoder(stream.toByteArray());

        assertArrayEquals(
                TestUtil.multiply(new boolean[]{true, true, false}, 100),
                decoder.readFlagArray()
        );
    }

    @Test
    public void shouldEncodeDecodeFlagVector() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        Encoder encoder = new Encoder(stream);
        encoder.writeFlagVector(new boolean[]{true, true, false});

        Decoder decoder = new Decoder(stream.toByteArray());

        assertArrayEquals(new boolean[]{true, true, false}, decoder.readFlagVector());
    }

    @Test
    public void shouldEncodeDecodeFloatArray() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        Encoder encoder = new Encoder(stream);
        encoder.writeFloatArray(TestUtil.multiply(new float[]{1.23f, 4.56f, 7.89f}, 100));

        Decoder decoder = new Decoder(stream.toByteArray());

        assertArrayEquals(
                TestUtil.multiply(new float[]{1.23f, 4.56f, 7.89f}, 100),
                decoder.readFloatArray(),
                0
        );
    }

    @Test
    public void shouldEncodeDecodeFloatVector() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        Encoder encoder = new Encoder(stream);
        encoder.writeFloatVector(new float[]{1.23f, 4.56f, 7.89f});

        Decoder decoder = new Decoder(stream.toByteArray());

        assertArrayEquals(new float[]{1.23f, 4.56f, 7.89f}, decoder.readFloatVector(), 0);
    }

    @Test
    public void shouldEncodeDecodeDoubleArray() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        Encoder encoder = new Encoder(stream);
        encoder.writeDoubleArray(TestUtil.multiply(new double[]{1.23, 4.56, 7.89}, 100));

        Decoder decoder = new Decoder(stream.toByteArray());

        assertArrayEquals(
                TestUtil.multiply(new double[]{1.23, 4.56, 7.89}, 100),
                decoder.readDoubleArray(),
                0
        );
    }

    @Test
    public void shouldEncodeDecodeDoubleVector() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        Encoder encoder = new Encoder(stream);
        encoder.writeDoubleVector(new double[]{1.23, 4.56, 7.89});

        Decoder decoder = new Decoder(stream.toByteArray());

        assertArrayEquals(new double[]{1.23, 4.56, 7.89}, decoder.readDoubleVector(), 0);
    }

    @Test
    public void shouldEncodeDecodeStringArray() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        Encoder encoder = new Encoder(stream);
        encoder.writeStringArray(TestUtil.multiply(new String[]{"a", "b", "c"}, 100));

        Decoder decoder = new Decoder(stream.toByteArray());

        assertArrayEquals(
                Arrays.stream(TestUtil.multiply(new String[]{"a", "b", "c"}, 100)).map(String::getBytes).toArray(),
                decoder.readStringArrayAsBytes()
        );
    }

    @Test
    public void shouldEncodeDecodeStringVector() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        Encoder encoder = new Encoder(stream);
        encoder.writeStringVector(new String[]{"a", "b", "c"});

        Decoder decoder = new Decoder(stream.toByteArray());

        assertArrayEquals(
                Arrays.stream(new String[]{"a", "b", "c"}).map(String::getBytes).toArray(),
                decoder.readStringVectorAsBytes()
        );
    }

    @Test
    public void shouldEncodeDecodeTextArray() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        Encoder encoder = new Encoder(stream);
        encoder.writeTextArray(TestUtil.multiply(new String[]{TestUtil.multiply("abc", 100)}, 300));

        Decoder decoder = new Decoder(stream.toByteArray());

        assertArrayEquals(
                Arrays.stream(TestUtil.multiply(new String[]{TestUtil.multiply("abc", 100)}, 300)).map(String::getBytes).toArray(),
                decoder.readTextArrayAsBytes()
        );
    }

    @Test
    public void shouldEncodeDecodeTextVector() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        Encoder encoder = new Encoder(stream);
        encoder.writeTextVector(new String[]{TestUtil.multiply("abc", 100)});

        Decoder decoder = new Decoder(stream.toByteArray());

        assertArrayEquals(
                Arrays.stream(new String[]{TestUtil.multiply("abc", 100)}).map(String::getBytes).toArray(),
                decoder.readTextVectorAsBytes()
        );
    }

}
