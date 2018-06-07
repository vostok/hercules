package ru.kontur.vostok.hercules.protocol.encoder;

import ru.kontur.vostok.hercules.protocol.decoder.SizeOf;
import ru.kontur.vostok.hercules.util.throwable.ThrowableUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static ru.kontur.vostok.hercules.util.throwable.ThrowableUtil.wrapException;


public class Encoder {

    private final ByteArrayOutputStream stream;

    public Encoder() {
        this.stream = new ByteArrayOutputStream();
    }

    public void writeByte(byte b) {
        try {
            stream.write(ByteBuffer.allocate(SizeOf.BYTE).put(b).array());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void writeUnsignedByte(int b) {
        try {
            stream.write(ByteBuffer.allocate(SizeOf.BYTE).put((byte) b).array());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void writeShort(short s) {
        try {
            stream.write(ByteBuffer.allocate(SizeOf.SHORT).putShort(s).array());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void writeInteger(int i) {
        try {
            stream.write(ByteBuffer.allocate(SizeOf.INTEGER).putInt(i).array());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void writeLong(long l) {
        try {
            stream.write(ByteBuffer.allocate(SizeOf.LONG).putLong(l).array());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void writeFloat(float f) {
        try {
            stream.write(ByteBuffer.allocate(SizeOf.FLOAT).putFloat(f).array());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void writeDouble(double d) {
        try {
            stream.write(ByteBuffer.allocate(SizeOf.DOUBLE).putDouble(d).array());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void writeFlag(boolean flag) {
        writeByte(flag ? (byte) 1 : (byte) 0);
    }

    public void writeString(String s) {
        try {
            byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
            writeVectorLength(bytes.length, "String bytes length must be lesser than 256");
            stream.write(bytes);
        } catch (IOException e) {
            throw  new RuntimeException(e);
        }
    }

    public void writeText(String s) {
        try {
            byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
            writeArrayLength(bytes.length);
            stream.write(bytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void writeByteArray(byte[] array) {
        try {
            writeArrayLength(array.length);
            stream.write(array);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void writeByteVector(byte[] vector) {
        try {
            writeVectorLength(vector.length);
            stream.write(vector);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void writeUnsignedByteArray(int[] array) {
        writeArrayLength(array.length);
        for(int ub : array) {
            writeUnsignedByte(ub);
        }
    }

    public void writeUnsignedByteVector(int[] vector) {
        writeVectorLength(vector.length);
        for (int ub : vector) {
            writeUnsignedByte(ub);
        }
    }

    public void writeShortArray(short[] array) {
        writeArrayLength(array.length);
        for(short s : array) {
            writeShort(s);
        }
    }

    public void writeShortVector(short[] vector) {
        writeVectorLength(vector.length);
        for (short s : vector) {
            writeShort(s);
        }
    }

    public void writeIntegerArray(int[] array) {
        writeArrayLength(array.length);
        for(int i: array) {
            writeInteger(i);
        }
    }

    public void writeIntegerVector(int[] vector) {
        writeVectorLength(vector.length);
        for (int i : vector) {
            writeInteger(i);
        }
    }

    public void writeLongArray(long[] array) {
        writeArrayLength(array.length);
        for(long l: array) {
            writeLong(l);
        }
    }

    public void writeLongVector(long[] vector) {
        writeVectorLength(vector.length);
        for (long l: vector) {
            writeLong(l);
        }
    }

    public void writeFlagArray(boolean[] array) {
        writeArrayLength(array.length);
        for(boolean b: array) {
            writeFlag(b);
        }
    }

    public void writeFlagVector(boolean[] vector) {
        writeVectorLength(vector.length);
        for (boolean b: vector) {
            writeFlag(b);
        }
    }

    public void writeFloatArray(float[] array) {
        writeArrayLength(array.length);
        for(float f: array) {
            writeFloat(f);
        }
    }

    public void writeFloatVector(float[] vector) {
        writeVectorLength(vector.length);
        for (float f: vector) {
            writeFloat(f);
        }
    }

    public void writeDoubleArray(double[] array) {
        writeArrayLength(array.length);
        for(double d: array) {
            writeDouble(d);
        }
    }

    public void writeDoubleVector(double[] vector) {
        writeVectorLength(vector.length);
        for (double d: vector) {
            writeDouble(d);
        }
    }

    public void writeStringArray(String[] array) {
        writeArrayLength(array.length);
        for(String s: array) {
            writeString(s);
        }
    }

    public void writeStringVector(String[] vector) {
        writeVectorLength(vector.length);
        for (String s: vector) {
            writeString(s);
        }
    }

    public void writeTextArray(String[] array) {
        writeArrayLength(array.length);
        for(String s: array) {
            writeText(s);
        }
    }

    public void writeTextVector(String[] vector) {
        writeVectorLength(vector.length);
        for (String s: vector) {
            writeText(s);
        }
    }

    public byte[] getBytes() {
        // TODO: Copy of byte array created here.
        return stream.toByteArray();
    }

    private void writeArrayLength(int length) {
        writeInteger(length);
    }

    private void writeVectorLength(int length, String errorMessage) {
        if (length < 256) {
            writeUnsignedByte(length);
        } else {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    private void writeVectorLength(int length) {
        writeVectorLength(length, "Vector length must be lesser than 256 items");
    }
}
