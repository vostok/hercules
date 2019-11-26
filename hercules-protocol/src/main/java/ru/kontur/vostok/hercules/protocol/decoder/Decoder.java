package ru.kontur.vostok.hercules.protocol.decoder;

import ru.kontur.vostok.hercules.protocol.TinyString;
import ru.kontur.vostok.hercules.protocol.Type;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Low-level protocol decoder.
 * <p>
 * {@link Decoder} decodes values of types are defined in enum {@link ru.kontur.vostok.hercules.protocol.Type}
 * or their parts (length of {@link ru.kontur.vostok.hercules.protocol.Type#STRING},
 * {@link ru.kontur.vostok.hercules.protocol.Type#VECTOR}, tag names and so on).
 * <p>
 * Also, {@link Decoder} can be used to skip some values.
 *
 * @author Gregory Koshelev
 */
public class Decoder {
    private final ByteBuffer buffer;

    public Decoder(byte[] data) {
        this.buffer = ByteBuffer.wrap(data);
    }

    public Decoder(ByteBuffer buffer) {
        this.buffer = buffer;
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
        return readBytes(length);
    }

    public UUID readUuid() {
        return new UUID(readLong(), readLong());
    }

    public Object readNull() {
        return null;
    }

    public Type readType() {
        return Type.valueOf(readByte());
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
        return skip(Type.BYTE.size);
    }

    public int skipShort() {
        return skip(Type.SHORT.size);
    }

    public int skipInteger() {
        return skip(Type.INTEGER.size);
    }

    public int skipLong() {
        return skip(Type.LONG.size);
    }

    public int skipFlag() {
        return skip(Type.FLAG.size);
    }

    public int skipFloat() {
        return skip(Type.FLOAT.size);
    }

    public int skipDouble() {
        return skip(Type.DOUBLE.size);
    }

    public int skipString() {
        int position = position();

        int length = readStringLength();
        skip(length);

        return position() - position;
    }

    public int skipUuid() {
        return skip(Type.UUID.size);
    }

    public int skipNull() {
        return Type.NULL.size;
    }

    public int skipByteVector() {
        int position = position();

        int length = readVectorLength();
        int bytesToSkip = length * Type.BYTE.size;
        skip(bytesToSkip);

        return position() - position;
    }

    public int skipShortVector() {
        int position = position();

        int length = readVectorLength();
        int bytesToSkip = length * Type.SHORT.size;
        skip(bytesToSkip);

        return position() - position;
    }

    public int skipIntegerVector() {
        int position = position();

        int length = readVectorLength();
        int bytesToSkip = length * Type.INTEGER.size;
        skip(bytesToSkip);

        return position() - position;
    }

    public int skipLongVector() {
        int position = position();

        int length = readVectorLength();
        int bytesToSkip = length * Type.LONG.size;
        skip(bytesToSkip);

        return position() - position;
    }

    public int skipFlagVector() {
        int position = position();

        int length = readVectorLength();
        int bytesToSkip = length * Type.FLAG.size;
        skip(bytesToSkip);

        return position() - position;
    }

    public int skipFloatVector() {
        int position = position();

        int length = readVectorLength();
        int bytesToSkip = length * Type.FLOAT.size;
        skip(bytesToSkip);

        return position() - position;
    }

    public int skipDoubleVector() {
        int position = position();

        int length = readVectorLength();
        int bytesToSkip = length * Type.DOUBLE.size;
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
     * @return tiny string
     */
    public TinyString readTinyString() {
        int length = readUnsignedByte();
        return TinyString.of(readBytes(length));
    }

    public int skipTinyString() {
        int length = readUnsignedByte();
        skip(length);
        return length + Type.BYTE.size;
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

    public int skip(int bytesToSkip) {
        buffer.position(buffer.position() + bytesToSkip);
        return bytesToSkip;
    }

    /**
     * Return an array of bytes that is sub array of underlying buffer.
     * <p>
     * The sub array begins at the index {@code from} and ends to the index {@code toExclusive - 1}.
     * The length of the sub array is {@code toExclusive - from}.
     *
     * @param from        the beginning index, inclusive
     * @param toExclusive the ending index, exclusive
     * @return the sub array
     */
    public byte[] subarray(int from, int toExclusive) {
        int originalPosition = buffer.position();

        buffer.position(from);
        byte[] subarray = new byte[toExclusive - from];
        buffer.get(subarray);

        buffer.position(originalPosition);// Restore buffer's position

        return subarray;
    }


}
