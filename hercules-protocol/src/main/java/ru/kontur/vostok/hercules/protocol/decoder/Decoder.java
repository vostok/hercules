package ru.kontur.vostok.hercules.protocol.decoder;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;

/**
 * @author Gregory Koshelev
 */
public class Decoder {
    private final byte[] data;
    private final ByteBuffer buffer;

    public Decoder(byte[] data) {
        this.data = data;
        this.buffer = ByteBuffer.wrap(data);
    }

    /* --- Read data types --- */

    public byte readByte() {
        return buffer.get();
    }

    public int readUnsignedByte() {
        return buffer.get() & 0xFF;
    }

    public short readShort() {
        return buffer.getShort();
    }

    public int readUnsignedShort() {
        return readShort() & 0xFFFF;
    }

    public int readInteger() {
        return buffer.getInt();
    }

    public long readLong() {
        return buffer.getLong();
    }

    public boolean readFlag() {
        return buffer.get() != 0;
    }

    public float readFloat() {
        return buffer.getFloat();
    }

    public double readDouble() {
        return buffer.getDouble();
    }

    public String readString() {
        byte[] bytes = readStringAsBytes();
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public byte[] readStringAsBytes() {
        int length = readStringLength();
        byte[] bytes = new byte[length];
        buffer.get(bytes);
        return bytes;
    }

    public UUID readUuid() {
        return new UUID(readLong(), readLong());
    }

    public Object readNull() {
        return null;
    }

    public byte[] readByteVector() {
        int length = readVectorLength();
        return readBytes(length);
    }

    public byte[] readBytes(int count) {
        byte[] array = new byte[count];
        buffer.get(array, 0, count);
        return array;
    }

    public int[] readUnsignedByteVector() {
        int length = readVectorLength();
        return readUnsignedBytes(length);
    }

    public int[] readUnsignedBytes(int count) {
        int[] array = new int[count];
        for (int i = 0; i < count; i++) {
            array[i] = readUnsignedByte();
        }
        return array;
    }

    public short[] readShortVector() {
        int length = readVectorLength();
        return readShorts(length);
    }

    public short[] readShorts(int count) {
        short[] array = new short[count];
        for (int i = 0; i < count; i++) {
            array[i] = readShort();
        }
        return array;
    }

    public int[] readIntegerVector() {
        int length = readVectorLength();
        return readIntegers(length);
    }

    public int[] readIntegers(int count) {
        int[] array = new int[count];
        for (int i = 0; i < count; i++) {
            array[i] = readInteger();
        }
        return array;
    }

    public long[] readLongVector() {
        int length = readVectorLength();
        return readLongs(length);
    }

    public long[] readLongs(int count) {
        long[] array = new long[count];
        for (int i = 0; i < count; i++) {
            array[i] = readLong();
        }
        return array;
    }

    public boolean[] readFlagVector() {
        int length = readVectorLength();
        return readFlags(length);
    }

    public boolean[] readFlags(int count) {
        boolean[] array = new boolean[count];
        for (int i = 0; i < count; i++) {
            array[i] = readFlag();
        }
        return array;
    }

    public float[] readFloatVector() {
        int length = readVectorLength();
        return readFloats(length);
    }

    public float[] readFloats(int count) {
        float[] array = new float[count];
        for (int i = 0; i < count; i++) {
            array[i] = readFloat();
        }
        return array;
    }

    public double[] readDoubleVector() {
        int length = readVectorLength();
        return readDoubles(length);
    }

    public double[] readDoubles(int count) {
        double[] array = new double[count];
        for (int i = 0; i < count; i++) {
            array[i] = readDouble();
        }
        return array;
    }

    public byte[][] readStringVectorAsBytes() {
        int length = readVectorLength();
        return readStringsAsBytes(length);
    }

    public byte[][] readStringsAsBytes(int count) {
        byte[][] array = new byte[count][];
        for (int i = 0; i < count; i++) {
            array[i] = readStringAsBytes();
        }
        return array;
    }

    public UUID[] readUuidVector() {
        int length = readVectorLength();
        return readUuids(length);
    }

    public UUID[] readUuids(int count) {
        UUID[] array = new UUID[count];
        for (int i = 0; i < count; i++) {
            array[i] = readUuid();
        }
        return array;
    }

    public Object[] readNullVector() {
        int length = readVectorLength();
        return readNulls(length);
    }

    public Object[] readNulls(int count) {
        Object[] array = new Object[count];
        return array;
    }

    /* Skip methods */

    public int skipByte() {
        skip(SizeOf.BYTE);
        return SizeOf.BYTE;
    }

    public int skipShort() {
        skip(SizeOf.SHORT);
        return SizeOf.SHORT;
    }

    public int skipInteger() {
        skip(SizeOf.INTEGER);
        return SizeOf.INTEGER;
    }

    public int skipLong() {
        skip(SizeOf.LONG);
        return SizeOf.LONG;
    }

    public int skipFlag() {
        skip(SizeOf.FLAG);
        return SizeOf.FLAG;
    }

    public int skipFloat() {
        skip(SizeOf.FLOAT);
        return SizeOf.FLOAT;
    }

    public int skipDouble() {
        skip(SizeOf.DOUBLE);
        return SizeOf.DOUBLE;
    }

    public int skipString() {
        int position = position();

        int length = readStringLength();
        skip(length);

        return position() - position;
    }

    public int skipUuid() {
        skip(SizeOf.UUID);
        return SizeOf.UUID;
    }

    public int skipNull() {
        return 0;
    }

    public int skipByteVector() {
        int position = position();

        int length = readVectorLength();
        int bytesToSkip = length * SizeOf.BYTE;
        skip(bytesToSkip);

        return position() - position;
    }

    public int skipShortVector() {
        int position = position();

        int length = readVectorLength();
        int bytesToSkip = length * SizeOf.SHORT;
        skip(bytesToSkip);

        return position() - position;
    }

    public int skipIntegerVector() {
        int position = position();

        int length = readVectorLength();
        int bytesToSkip = length * SizeOf.INTEGER;
        skip(bytesToSkip);

        return position() - position;
    }

    public int skipLongVector() {
        int position = position();

        int length = readVectorLength();
        int bytesToSkip = length * SizeOf.LONG;
        skip(bytesToSkip);

        return position() - position;
    }

    public int skipFlagVector() {
        int position = position();

        int length = readVectorLength();
        int bytesToSkip = length * SizeOf.FLAG;
        skip(bytesToSkip);

        return position() - position;
    }

    public int skipFloatVector() {
        int position = position();

        int length = readVectorLength();
        int bytesToSkip = length * SizeOf.FLOAT;
        skip(bytesToSkip);

        return position() - position;
    }

    public int skipDoubleVector() {
        int position = position();

        int length = readVectorLength();
        int bytesToSkip = length * SizeOf.DOUBLE;
        skip(bytesToSkip);

        return position() - position;
    }

    public int skipStringVector() {
        int position = position();

        int length = readVectorLength();
        for (int i = 0; i < length; i++) {
            skipString();
        }

        return position() - position;
    }

    public int skipUuidVector() {
        int position = position();

        int length = readVectorLength();
        for (int i = 0; i < length; i++) {
            skipUuid();
        }

        return position() - position;
    }

    public int skipNullVector() {
        int position = position();

        readVectorLength();

        return position() - position;
    }

    /* --- Utility methods --- */

    /**
     * Read tiny string, which has 1-byte length
     *
     * @return string
     */
    public String readTinyString() {
        int length = readUnsignedByte();
        byte[] bytes = new byte[length];
        buffer.get(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public int skipTinyString() {
        int length = readUnsignedByte();
        skip(length);
        return length + SizeOf.BYTE;
    }

    public int readVarLen() {
        byte b = buffer.get();
        int value = b & 0x7F;
        while ((b & 0x80) == 0x80) {
            value = (value << 7);
            b = buffer.get();
            value |= (b & 0x7F);
        }
        return value;
    }

    public int readVectorLength() {
        return readInteger();
    }

    public int readStringLength() {
        return readInteger();
    }

    public int readContainerSize() {
        return readUnsignedShort();
    }

    public int position() {
        return buffer.position();
    }

    public void skip(int bytesToSkip) {
        buffer.position(buffer.position() + bytesToSkip);
    }

    public byte[] subarray(int from, int toExclusive) {
        return Arrays.copyOfRange(data, from, toExclusive);
    }


}
