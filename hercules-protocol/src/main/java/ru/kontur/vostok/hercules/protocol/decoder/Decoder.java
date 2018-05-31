package ru.kontur.vostok.hercules.protocol.decoder;

import ru.kontur.vostok.hercules.protocol.Type;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * @author Gregory Koshelev
 */
public class Decoder {
    private final ByteBuffer buffer;

    public Decoder(byte[] data) {
        this.buffer = ByteBuffer.wrap(data);
    }

    /**
     * Read value of type specified.
     * <p>
     * Type.BYTE as java.lang.Byte                                              <br>
     * Type.SHORT as java.lang.Short                                            <br>
     * Type.INTEGER as java.lang.Integer                                        <br>
     * Type.LONG as java.lang.Long                                              <br>
     * Type.FLAG as java.lang.Boolean                                           <br>
     * Type.FLOAT as java.lang.Float                                            <br>
     * Type.DOUBLE as java.lang.Double                                          <br>
     * Type.STRING as byte[] (array of UTF-8 bytes)                             <br>
     * Type.TEXT as byte[] (array of UTF-8 bytes)                               <br>
     * Type.BYTE_ARRAY as byte[]                                                <br>
     * Type.SHORT_ARRAY as short[]                                              <br>
     * Type.INTEGER_ARRAY as int[]                                              <br>
     * Type.LONG_ARRAY as long[]                                                <br>
     * Type.FLAG_ARRAY as boolean[]                                             <br>
     * Type.FLOAT_ARRAY as float[]                                              <br>
     * Type.DOUBLE_ARRAY as double[]                                            <br>
     * Type.STRING_ARRAY as byte[][] (array of array of UTF-8 bytes)            <br>
     * Type.TEXT_ARRAY as byte[] (array of array of UTF-8 bytes)                <br>
     * Type.BYTE_VECTOR as byte[]                                               <br>
     * Type.SHORT_VECTOR as short[]                                             <br>
     * Type.INTEGER_VECTOR as int[]                                             <br>
     * Type.LONG_VECTOR as long[]                                               <br>
     * Type.FLAG_VECTOR as boolean[]                                            <br>
     * Type.FLOAT_VECTOR as float[]                                             <br>
     * Type.DOUBLE_VECTOR as double[]                                           <br>
     * Type.STRING_VECTOR as byte[][] (array of array of UTF-8 bytes)           <br>
     * Type.TEXT_VECTOR as byte[] (array of array of UTF-8 bytes)               <br>
     * </p>
     * @param type of value to be read which defines decoder as described above
     * @return decoded value
     */
    public Object read(Type type) {
        return decoders[type.value].decode(this);
    }

    public int skip(Type type) {
        return skippers[type.value].skip(this);
    }

    /* --- Read data types --- */

    public Type readType() {
        return Type.valueOf(buffer.get());
    }

    public byte readByte() {
        return buffer.get();
    }

    public int readUnsignedByte() {
        return buffer.get() & 0xFF;
    }

    public short readShort() {
        return buffer.getShort();
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
        int length = readUnsignedByte();
        byte[] bytes = new byte[length];
        buffer.get(bytes);
        return bytes;
    }

    public String readText() {
        byte[] bytes = readTextAsBytes();
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public byte[] readTextAsBytes() {
        int length = readInteger();
        byte[] bytes = new byte[length];
        buffer.get(bytes);
        return bytes;
    }

    public byte[] readByteArray() {
        int length = readArrayLength();
        return readBytes(length);
    }

    public byte[] readByteVector() {
        int length = readVectorLength();
        return readBytes(length);
    }

    public byte[] readBytes(int count) {
        byte[] array = new byte[count];
        buffer.get(array);
        return array;
    }

    public int[] readUnsignedByteArray() {
        int length = readArrayLength();
        return readUnsignedBytes(length);
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

    public short[] readShortArray() {
        int length = readArrayLength();
        return readShorts(length);
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

    public int[] readIntegerArray() {
        int length = readArrayLength();
        return readIntegers(length);
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

    public long[] readLongArray() {
        int length = readArrayLength();
        return readLongs(length);
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

    public boolean[] readFlagArray() {
        int length = readArrayLength();
        return readFlags(length);
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

    public float[] readFloatArray() {
        int length = readArrayLength();
        return readFloats(length);
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

    public double[] readDoubleArray() {
        int length = readArrayLength();
        return readDoubles(length);
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

    public byte[][] readStringArrayAsBytes() {
        int length = readArrayLength();
        return readStringsAsBytes(length);
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

    public byte[][] readTextArrayAsBytes() {
        int length = readArrayLength();
        return readTextsAsBytes(length);
    }

    public byte[][] readTextVectorAsBytes() {
        int length = readVectorLength();
        return readTextsAsBytes(length);
    }

    public byte[][] readTextsAsBytes(int count) {
        byte[][] array = new byte[count][];
        for (int i = 0; i < count; i++) {
            array[i] = readTextAsBytes();
        }
        return array;
    }

    /* Skip methods */

    public int skipByte() {
        skip(SIZEOF_BYTE);
        return SIZEOF_BYTE;
    }

    public int skipShort() {
        skip(SIZEOF_SHORT);
        return SIZEOF_SHORT;
    }

    public int skipInteger() {
        skip(SIZEOF_INTEGER);
        return SIZEOF_INTEGER;
    }

    public int skipLong() {
        skip(SIZEOF_LONG);
        return SIZEOF_LONG;
    }

    public int skipFlag() {
        skip(SIZEOF_FLAG);
        return SIZEOF_FLAG;
    }

    public int skipFloat() {
        skip(SIZEOF_FLOAT);
        return SIZEOF_FLOAT;
    }

    public int skipDouble() {
        skip(SIZEOF_DOUBLE);
        return SIZEOF_DOUBLE;
    }

    public int skipString() {
        int length = readUnsignedByte();
        skip(length);

        return length + SIZEOF_STRING_LENGTH;
    }

    public int skipText() {
        int length = readInteger();
        skip(length);

        return length + SIZEOF_TEXT_LENGTH;
    }

    public int skipByteArray() {
        int length = readArrayLength();
        int bytesToSkip = length * SIZEOF_BYTE;
        skip(bytesToSkip);

        return bytesToSkip + SIZEOF_ARRAY_LENGTH;
    }

    public int skipByteVector() {
        int length = readVectorLength();
        int bytesToSkip = length * SIZEOF_BYTE;
        skip(bytesToSkip);

        return bytesToSkip + SIZEOF_VECTOR_LENGTH;
    }

    public int skipShortArray() {
        int length = readArrayLength();
        int bytesToSkip = length * SIZEOF_SHORT;
        skip(bytesToSkip);

        return bytesToSkip + SIZEOF_ARRAY_LENGTH;
    }

    public int skipShortVector() {
        int length = readVectorLength();
        int bytesToSkip = length * SIZEOF_SHORT;
        skip(bytesToSkip);

        return bytesToSkip + SIZEOF_VECTOR_LENGTH;
    }

    public int skipIntegerArray() {
        int length = readArrayLength();
        int bytesToSkip = length * SIZEOF_INTEGER;
        skip(bytesToSkip);

        return bytesToSkip + SIZEOF_ARRAY_LENGTH;
    }

    public int skipIntegerVector() {
        int length = readVectorLength();
        int bytesToSkip = length * SIZEOF_INTEGER;
        skip(bytesToSkip);

        return bytesToSkip + SIZEOF_VECTOR_LENGTH;
    }

    public int skipLongArray() {
        int length = readArrayLength();
        int bytesToSkip = length * SIZEOF_LONG;
        skip(bytesToSkip);

        return bytesToSkip + SIZEOF_ARRAY_LENGTH;
    }

    public int skipLongVector() {
        int length = readVectorLength();
        int bytesToSkip = length * SIZEOF_LONG;
        skip(bytesToSkip);

        return bytesToSkip + SIZEOF_VECTOR_LENGTH;
    }

    public int skipFlagArray() {
        int length = readArrayLength();
        int bytesToSkip = length * SIZEOF_FLAG;
        skip(bytesToSkip);

        return bytesToSkip + SIZEOF_ARRAY_LENGTH;
    }

    public int skipFlagVector() {
        int length = readVectorLength();
        int bytesToSkip = length * SIZEOF_FLAG;
        skip(bytesToSkip);

        return bytesToSkip + SIZEOF_VECTOR_LENGTH;
    }

    public int skipFloatArray() {
        int length = readArrayLength();
        int bytesToSkip = length * SIZEOF_FLOAT;
        skip(bytesToSkip);

        return bytesToSkip + SIZEOF_ARRAY_LENGTH;
    }

    public int skipFloatVector() {
        int length = readVectorLength();
        int bytesToSkip = length * SIZEOF_FLOAT;
        skip(bytesToSkip);

        return bytesToSkip + SIZEOF_VECTOR_LENGTH;
    }

    public int skipDoubleArray() {
        int length = readArrayLength();
        int bytesToSkip = length * SIZEOF_DOUBLE;
        skip(bytesToSkip);

        return bytesToSkip + SIZEOF_ARRAY_LENGTH;
    }

    public int skipDoubleVector() {
        int length = readVectorLength();
        int bytesToSkip = length * SIZEOF_DOUBLE;
        skip(bytesToSkip);

        return bytesToSkip + SIZEOF_VECTOR_LENGTH;
    }

    public int skipStringArray() {
        int length = readArrayLength();
        int skipped = 0;
        for (int i = 0; i < length; i++) {
            skipped += skipString();
        }

        return skipped + SIZEOF_ARRAY_LENGTH;
    }

    public int skipStringVector() {
        int length = readVectorLength();
        int skipped = 0;
        for (int i = 0; i < length; i++) {
            skipped += skipString();
        }

        return skipped + SIZEOF_VECTOR_LENGTH;
    }

    public int skipTextArray() {
        int length = readArrayLength();
        int skipped = 0;
        for (int i = 0; i < length; i++) {
            skipped += skipText();
        }

        return skipped + SIZEOF_ARRAY_LENGTH;
    }

    public int skipTextVector() {
        int length = readVectorLength();
        int skipped = 0;
        for (int i = 0; i < length; i++) {
            skipped += skipText();
        }

        return skipped + SIZEOF_VECTOR_LENGTH;
    }

    /* --- Utility methods --- */

    public int readArrayLength() {
        return readInteger();
    }

    public int readVectorLength() {
        return readUnsignedByte();
    }

    public int position() {
        return buffer.position();
    }

    public void skip(int bytesToSkip) {
        buffer.position(buffer.position() + bytesToSkip);
    }

    /* --- sizeof --- */

    private final static int SIZEOF_BYTE = 1;
    private final static int SIZEOF_SHORT = 2;
    private final static int SIZEOF_INTEGER = 4;
    private final static int SIZEOF_LONG = 8;
    private final static int SIZEOF_FLAG = 1;
    private final static int SIZEOF_FLOAT = 4;
    private final static int SIZEOF_DOUBLE = 8;
    private final static int SIZEOF_STRING_LENGTH = 1;
    private final static int SIZEOF_TEXT_LENGTH = 4;
    private final static int SIZEOF_VECTOR_LENGTH = 1;
    private final static int SIZEOF_ARRAY_LENGTH = 4;

    /**
     * Type decoders
     */
    private final static TypeDecoder[] decoders = {
            decoder -> null,
            Decoder::readByte,
            Decoder::readShort,
            Decoder::readInteger,
            Decoder::readLong,
            Decoder::readFlag,
            Decoder::readFloat,
            Decoder::readDouble,
            Decoder::readStringAsBytes,
            Decoder::readTextAsBytes,
            decoder -> null,
            Decoder::readByteArray,
            Decoder::readShortArray,
            Decoder::readIntegerArray,
            Decoder::readLongArray,
            Decoder::readFlagArray,
            Decoder::readFloatArray,
            Decoder::readDoubleArray,
            Decoder::readStringArrayAsBytes,
            Decoder::readTextArrayAsBytes,
            decoder -> null,
            Decoder::readByteVector,
            Decoder::readShortVector,
            Decoder::readIntegerVector,
            Decoder::readLongVector,
            Decoder::readFlagVector,
            Decoder::readFloatVector,
            Decoder::readDoubleVector,
            Decoder::readStringVectorAsBytes,
            Decoder::readTextVectorAsBytes,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
            decoder -> null,
    };

    /**
     * Skip methods
     */
    private static TypeSkipper[] skippers = {
            decoder -> 0,
            Decoder::skipByte,
            Decoder::skipShort,
            Decoder::skipInteger,
            Decoder::skipLong,
            Decoder::skipFlag,
            Decoder::skipFloat,
            Decoder::skipDouble,
            Decoder::skipString,
            Decoder::skipText,
            decoder -> 0,
            Decoder::skipByteArray,
            Decoder::skipShortArray,
            Decoder::skipIntegerArray,
            Decoder::skipLongArray,
            Decoder::skipFlagArray,
            Decoder::skipFloatArray,
            Decoder::skipDoubleArray,
            Decoder::skipStringArray,
            Decoder::skipTextArray,
            decoder -> 0,
            Decoder::skipByteVector,
            Decoder::skipShortVector,
            Decoder::skipIntegerVector,
            Decoder::skipLongVector,
            Decoder::skipFlagVector,
            Decoder::skipFloatVector,
            Decoder::skipDoubleVector,
            Decoder::skipStringVector,
            Decoder::skipTextVector,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
            decoder -> 0,
    };
}