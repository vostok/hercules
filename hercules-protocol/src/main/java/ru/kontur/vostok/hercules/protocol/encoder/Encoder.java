package ru.kontur.vostok.hercules.protocol.encoder;

import ru.kontur.vostok.hercules.protocol.TinyString;
import ru.kontur.vostok.hercules.protocol.Type;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Low-level protocol encoder.
 * <p>
 * Encoder uses {@link ByteBuffer} to encode the data.
 * Thus, buffer should have enough free space to proceed encoding.
 * <p>
 * Encoder avoids memory allocations except for string encoding
 * where {@link String#getBytes(Charset)} is used to obtain UTF-8 bytes of string.
 *
 * @author Gregory Koshelev
 */
public class Encoder {
    private final ByteBuffer buffer;

    public Encoder(ByteBuffer buffer) {
        this.buffer = buffer;
    }

    public void writeByte(byte b) {
        buffer.put(b);
    }

    public void writeUnsignedByte(int b) {
        buffer.put((byte) b);
    }

    public void writeShort(short s) {
        buffer.putShort(s);
    }

    public void writeUnsignedShort(int s) {
        buffer.putShort((short) s);
    }

    public void writeInteger(int i) {
        buffer.putInt(i);
    }

    public void writeLong(long l) {
        buffer.putLong(l);
    }

    public void writeFlag(boolean flag) {
        writeByte(flag ? (byte) 1 : (byte) 0);
    }

    public void writeFloat(float f) {
        buffer.putFloat(f);
    }

    public void writeDouble(double d) {
        buffer.putDouble(d);
    }

    public void writeString(String s) {
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        writeBytesAsString(bytes);
    }

    public void writeBytesAsString(byte[] bytes) {
        writeStringLength(bytes.length);
        writeRawBytes(bytes);
    }

    public void writeUuid(UUID uuid) {
        writeLong(uuid.getMostSignificantBits());
        writeLong(uuid.getLeastSignificantBits());
    }

    public void writeNull() {
        // Write null is no-op
    }

    public void writeType(Type type) {
        writeUnsignedByte(type.code);
    }

    public void writeByteVector(byte[] vector) {
        writeVectorLength(vector.length);
        writeRawBytes(vector);
    }

    public void writeUnsignedByteVector(int[] vector) {
        writeVectorLength(vector.length);
        for (int ub : vector) {
            writeUnsignedByte(ub);
        }
    }

    public void writeShortVector(short[] vector) {
        writeVectorLength(vector.length);
        for (short s : vector) {
            writeShort(s);
        }
    }

    public void writeIntegerVector(int[] vector) {
        writeVectorLength(vector.length);
        for (int i : vector) {
            writeInteger(i);
        }
    }

    public void writeLongVector(long[] vector) {
        writeVectorLength(vector.length);
        for (long l : vector) {
            writeLong(l);
        }
    }

    public void writeFlagVector(boolean[] vector) {
        writeVectorLength(vector.length);
        for (boolean b : vector) {
            writeFlag(b);
        }
    }

    public void writeFloatVector(float[] vector) {
        writeVectorLength(vector.length);
        for (float f : vector) {
            writeFloat(f);
        }
    }

    public void writeDoubleVector(double[] vector) {
        writeVectorLength(vector.length);
        for (double d : vector) {
            writeDouble(d);
        }
    }

    public void writeStringVector(String[] vector) {
        writeVectorLength(vector.length);
        for (String s : vector) {
            writeString(s);
        }
    }

    public void writeBytesAsStringVector(byte[][] strings) {
        writeVectorLength(strings.length);
        for (byte[] string : strings) {
            writeBytesAsString(string);
        }
    }

    public void writeUuidVector(UUID[] vector) {
        writeVectorLength(vector.length);
        for (UUID uuid : vector) {
            writeUuid(uuid);
        }
    }

    public void writeNullVector(Object[] vector) {
        writeVectorLength(vector.length);
        /* // Writing null is no-op
           for (int i = 0; i < vector.length; i++) {
               writeNull();
           }
        */
    }

    /* --- Utility methods --- */

    public void writeRawBytes(byte[] bytes) {
        buffer.put(bytes);
    }

    public void writeTinyString(TinyString ts) {
        writeUnsignedByte(ts.length());
        buffer.put(ts.getBytes());
    }
    /**
     * Write tiny string, which has 1-byte length
     *
     * @param s is tiny string
     */
    public void writeTinyString(String s) {
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > 255) {
            throw new IllegalArgumentException("Length of tiny string should be less or equal 255 but got " + bytes.length);
        }
        writeUnsignedByte(bytes.length);
        buffer.put(bytes);
    }

    public void writeVectorLength(int length) {
        writeInteger(length);
    }

    public void writeStringLength(int length) {
        writeInteger(length);
    }

    public void writeContainerSize(int size) {
        writeUnsignedShort(size);
    }
}
