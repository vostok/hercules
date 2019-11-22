package ru.kontur.vostok.hercules.protocol;


import org.junit.Before;
import org.junit.Test;
import ru.kontur.vostok.hercules.protocol.decoder.Decoder;
import ru.kontur.vostok.hercules.protocol.encoder.Encoder;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.UUID;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class EncoderDecoderTest {
    private ByteBuffer buffer = ByteBuffer.allocate(1024);

    @Before
    public void before() {
        buffer.clear();
    }

    @Test
    public void shouldEncodeDecodeByte() {
        Encoder encoder = new Encoder(buffer);
        encoder.writeByte((byte) 0xDE);

        buffer.flip();
        Decoder decoder = new Decoder(buffer);

        assertEquals((byte) 0xDE, decoder.readByte());
    }

    @Test
    public void shouldEncodeUnsignedByte() {
        Encoder encoder = new Encoder(buffer);
        encoder.writeUnsignedByte(0xDF);

        buffer.flip();
        Decoder decoder = new Decoder(buffer);

        assertEquals(0xDF, decoder.readUnsignedByte());
    }

    @Test
    public void shouldEncodeDecodeShort() {
        Encoder encoder = new Encoder(buffer);
        encoder.writeShort((short) 10_000);

        buffer.flip();
        Decoder decoder = new Decoder(buffer);

        assertEquals((short) 10_000, decoder.readShort());
    }

    @Test
    public void shouldEncodeDecodeInt() {
        Encoder encoder = new Encoder(buffer);
        encoder.writeInteger(0xDEADBEEF);

        buffer.flip();
        Decoder decoder = new Decoder(buffer);

        assertEquals(0xDEADBEEF, decoder.readInteger());
    }

    @Test
    public void shouldEncodeDecodeLong() {
        Encoder encoder = new Encoder(buffer);
        encoder.writeLong(0xDEADDEADBEEFBEEFL);

        buffer.flip();
        Decoder decoder = new Decoder(buffer);

        assertEquals(0xDEADDEADBEEFBEEFL, decoder.readLong());
    }

    @Test
    public void shouldEncodeDecodeFloat() {
        Encoder encoder = new Encoder(buffer);
        encoder.writeFloat(123.456f);

        buffer.flip();
        Decoder decoder = new Decoder(buffer);

        assertEquals(123.456f, decoder.readFloat(), 0);
    }

    @Test
    public void shouldEncodeDecodeDouble() {
        Encoder encoder = new Encoder(buffer);
        encoder.writeDouble(0.123456789);

        buffer.flip();
        Decoder decoder = new Decoder(buffer);

        assertEquals(0.123456789, decoder.readDouble(), 0);
    }

    @Test
    public void shouldEncodeDecodeFlag() {
        Encoder trueEncoder = new Encoder(buffer);
        trueEncoder.writeFlag(true);

        buffer.flip();
        Decoder trueDecoder = new Decoder(buffer);
        assertTrue(trueDecoder.readFlag());

        buffer.clear();//TODO: Better split the test into two tests
        Encoder falseEncoder = new Encoder(buffer);
        falseEncoder.writeFlag(false);

        buffer.flip();
        Decoder falseDecoder = new Decoder(buffer);
        assertFalse(falseDecoder.readFlag());
    }

    @Test
    public void shouldEncodeDecodeString() {
        Encoder encoder = new Encoder(buffer);
        encoder.writeString(
                "A sample string with UTF: АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧЩЪЫЬЭЮЯабвгдеёжхийклмнопрстуфхцчшщъыьэюя"
        );

        buffer.flip();
        Decoder decoder = new Decoder(buffer);

        assertEquals(
                "A sample string with UTF: АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧЩЪЫЬЭЮЯабвгдеёжхийклмнопрстуфхцчшщъыьэюя",
                decoder.readString()
        );
    }

    @Test
    public void shouldEncodeDecodeUuid() {
        UUID uuid = UUID.fromString("11203800-63FD-11E8-83E2-3A587D902000");

        Encoder encoder = new Encoder(buffer);
        encoder.writeUuid(uuid);

        buffer.flip();
        Decoder decoder = new Decoder(buffer);

        assertEquals(uuid.toString(), decoder.readUuid().toString());
    }

    @Test
    public void shouldEncodeDecodeNull() {
        Encoder encoder = new Encoder(buffer);
        encoder.writeNull();

        assertEquals(0, buffer.position());

        buffer.flip();
        Decoder decoder = new Decoder(buffer);

        assertNull(decoder.readNull());
        assertEquals(0, buffer.position());
    }

    @Test
    public void shouldEncodeDecodeByteVector() {
        Encoder encoder = new Encoder(buffer);
        encoder.writeByteVector(new byte[]{(byte) 1, (byte) 2, (byte) 3});

        buffer.flip();
        Decoder decoder = new Decoder(buffer);

        assertArrayEquals(new byte[]{(byte) 1, (byte) 2, (byte) 3}, decoder.readByteVector());
    }

    @Test
    public void shouldEncodeDecodeUnsignedByteVector() {
        Encoder encoder = new Encoder(buffer);
        encoder.writeUnsignedByteVector(new int[]{0, 100, 200});

        buffer.flip();
        Decoder decoder = new Decoder(buffer);

        assertArrayEquals(new int[]{0, 100, 200}, decoder.readUnsignedByteVector());
    }

    @Test
    public void shouldEncodeDecodeShortVector() {
        Encoder encoder = new Encoder(buffer);
        encoder.writeShortVector(new short[]{100, 10_000, 20_000});

        buffer.flip();
        Decoder decoder = new Decoder(buffer);

        assertArrayEquals(new short[]{100, 10_000, 20_000}, decoder.readShortVector());
    }

    @Test
    public void shouldEncodeDecodeIntegerVector() {
        Encoder encoder = new Encoder(buffer);
        encoder.writeIntegerVector(new int[]{1_000, 10_000, 100_000});

        buffer.flip();
        Decoder decoder = new Decoder(buffer);

        assertArrayEquals(new int[]{1_000, 10_000, 100_000}, decoder.readIntegerVector());
    }

    @Test
    public void shouldEncodeDecodeLongVector() {
        Encoder encoder = new Encoder(buffer);
        encoder.writeLongVector(new long[]{1_000, 10_000, 100_000});

        buffer.flip();
        Decoder decoder = new Decoder(buffer);

        assertArrayEquals(new long[]{1_000, 10_000, 100_000}, decoder.readLongVector());
    }

    @Test
    public void shouldEncodeDecodeFlagVector() {
        Encoder encoder = new Encoder(buffer);
        encoder.writeFlagVector(new boolean[]{true, true, false});

        buffer.flip();
        Decoder decoder = new Decoder(buffer);

        assertArrayEquals(new boolean[]{true, true, false}, decoder.readFlagVector());
    }

    @Test
    public void shouldEncodeDecodeFloatVector() {
        Encoder encoder = new Encoder(buffer);
        encoder.writeFloatVector(new float[]{1.23f, 4.56f, 7.89f});

        buffer.flip();
        Decoder decoder = new Decoder(buffer);

        assertArrayEquals(new float[]{1.23f, 4.56f, 7.89f}, decoder.readFloatVector(), 0);
    }

    @Test
    public void shouldEncodeDecodeDoubleVector() {
        Encoder encoder = new Encoder(buffer);
        encoder.writeDoubleVector(new double[]{1.23, 4.56, 7.89});

        buffer.flip();
        Decoder decoder = new Decoder(buffer);

        assertArrayEquals(new double[]{1.23, 4.56, 7.89}, decoder.readDoubleVector(), 0);
    }

    @Test
    public void shouldEncodeDecodeStringVector() {
        Encoder encoder = new Encoder(buffer);
        encoder.writeStringVector(new String[]{"a", "b", "c"});

        buffer.flip();
        Decoder decoder = new Decoder(buffer);

        assertArrayEquals(
                Arrays.stream(new String[]{"a", "b", "c"}).map(String::getBytes).toArray(),
                decoder.readStringVectorAsBytes()
        );
    }

    @Test
    public void shouldEncodeDecodeUuidVector() {
        UUID[] uuids = new UUID[]{UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()};

        Encoder encoder = new Encoder(buffer);
        encoder.writeUuidVector(uuids);

        buffer.flip();
        Decoder decoder = new Decoder(buffer);

        assertArrayEquals(uuids, decoder.readUuidVector()
        );
    }

    @Test
    public void shouldEncodeDecodeNullVector() {
        Object[] nulls = new Object[] {null, null, null};

        Encoder encoder = new Encoder(buffer);
        encoder.writeNullVector(nulls);

        buffer.flip();
        Decoder decoder = new Decoder(buffer);

        assertArrayEquals(nulls, decoder.readNullVector());
    }
}
