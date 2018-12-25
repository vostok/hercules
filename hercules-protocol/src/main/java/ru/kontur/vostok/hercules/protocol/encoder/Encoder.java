package ru.kontur.vostok.hercules.protocol.encoder;

import ru.kontur.vostok.hercules.protocol.Type;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;


public class Encoder {

    private final DataOutputStream stream;

    public Encoder(OutputStream stream) {
        this.stream = new DataOutputStream(stream);
    }

    public void writeByte(byte b) {
        try {
            stream.writeByte(b);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void writeUnsignedByte(int b) {
        try {
            stream.write((byte) b);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void writeShort(short s) {
        try {
            stream.writeShort(s);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void writeUnsignedShort(int s) {
        try {
            stream.writeShort(s);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void writeInteger(int i) {
        try {
            stream.writeInt(i);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void writeLong(long l) {
        try {
            stream.writeLong(l);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void writeFlag(boolean flag) {
        writeByte(flag ? (byte) 1 : (byte) 0);
    }

    public void writeFloat(float f) {
        try {
            stream.writeFloat(f);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void writeDouble(double d) {
        try {
            stream.writeDouble(d);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void writeString(String s) {
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        writeBytesAsString(bytes);
    }

    public void writeBytesAsString(byte[] bytes) {
        try {
            writeStringLength(bytes.length);
            stream.write(bytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void writeUuid(UUID uuid) {
        try {
            stream.writeLong(uuid.getMostSignificantBits());
            stream.writeLong(uuid.getLeastSignificantBits());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void writeNull() {
    }

    public void writeType(Type type) {
        writeUnsignedByte(type.code);
    }

    public void writeByteVector(byte[] vector) {
        try {
            writeVectorLength(vector.length);
            stream.write(vector);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
        for(int i = 0; i < vector.length; i++) {
            writeNull();
        }
    }

    public void writeRawBytes(byte[] bytes) {
        try {
            stream.write(bytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /* --- Utility methods --- */

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
        try {
            stream.write(bytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void writeVarLen(int value) {
        if (value < 0) {
            throw new IllegalArgumentException("Cannot encode negative value: " + value);
        }

        try {
            byte lsb1 = (byte) (value & 0x7F);
            value = value >> 7;
            if (value == 0) {
                stream.write(lsb1);
                return;
            }

            byte lsb2 = (byte) (value & 0x7F);
            value = value >> 7;
            if (value == 0) {
                stream.write(lsb2 | 0x80);
                stream.write(lsb1);
                return;
            }

            byte lsb3 = (byte) (value & 0x7F);
            value = value >> 7;
            if (value == 0) {
                stream.write(lsb3 | 0x80);
                stream.write(lsb2 | 0x80);
                stream.write(lsb1);
                return;
            }

            byte lsb4 = (byte) (value & 0x7F);
            value = value >> 7;
            if (value == 0) {
                stream.write(lsb4 | 0x80);
                stream.write(lsb3 | 0x80);
                stream.write(lsb2 | 0x80);
                stream.write(lsb1);
                return;
            }

            byte lsb5 = (byte) (value & 0x7F);
            stream.write(lsb5 | 0x80);
            stream.write(lsb4 | 0x80);
            stream.write(lsb3 | 0x80);
            stream.write(lsb2 | 0x80);
            stream.write(lsb1);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
