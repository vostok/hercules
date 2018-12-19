package ru.kontur.vostok.hercules.protocol.decoder;

import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Type;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.protocol.Vector;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.function.Function;
import java.util.function.ToIntFunction;

public class VariantReader implements Reader<Variant> {

    public static final VariantReader INSTANCE = new VariantReader();
    private static final ContainerReader CONTAINER_READER = ContainerReader.INSTANCE;
    private static final ContainerVectorReader CONTAINER_VECTOR_READER = ContainerVectorReader.INSTANCE;
    /**
     * Scalar decoders
     */
    private static final ObjectReader[] DECODERS = new ObjectReader[256];
    /**
     * Vector decoders
     */
    private static final ObjectReader[] VECTOR_DECODERS = new ObjectReader[256];
    /**
     * Skip scalar methods
     */
    private static final ObjectSkipper[] SKIPPERS = new ObjectSkipper[256];
    /**
     * Skip vector methods
     */
    private static final ObjectSkipper[] VECTOR_SKIPPERS = new ObjectSkipper[256];

    static {
        Arrays.setAll(DECODERS, idx -> decoder -> {
            throw new IllegalArgumentException("Unknown type with code " + String.valueOf(idx));
        });

        DECODERS[Type.CONTAINER.code] = VariantReader::readContainer;
        DECODERS[Type.BYTE.code] = Decoder::readByte;
        DECODERS[Type.SHORT.code] = Decoder::readShort;
        DECODERS[Type.INTEGER.code] = Decoder::readInteger;
        DECODERS[Type.LONG.code] = Decoder::readLong;
        DECODERS[Type.FLAG.code] = Decoder::readFlag;
        DECODERS[Type.FLOAT.code] = Decoder::readFloat;
        DECODERS[Type.DOUBLE.code] = Decoder::readDouble;
        DECODERS[Type.STRING.code] = Decoder::readStringAsBytes;
        DECODERS[Type.UUID.code] = Decoder::readUuid;
        DECODERS[Type.NULL.code] = Decoder::readNull;
        DECODERS[Type.VECTOR.code] = VariantReader::readVector;
    }

    static {
        Arrays.setAll(VECTOR_DECODERS, idx -> decoder -> {
            throw new IllegalArgumentException("Unknown type with code " + String.valueOf(idx));
        });

        VECTOR_DECODERS[Type.CONTAINER.code] = VariantReader::readContainerVector;
        VECTOR_DECODERS[Type.BYTE.code] = Decoder::readByteVector;
        VECTOR_DECODERS[Type.SHORT.code] = Decoder::readShortVector;
        VECTOR_DECODERS[Type.INTEGER.code] = Decoder::readIntegerVector;
        VECTOR_DECODERS[Type.LONG.code] = Decoder::readLongVector;
        VECTOR_DECODERS[Type.FLAG.code] = Decoder::readFlagVector;
        VECTOR_DECODERS[Type.FLOAT.code] = Decoder::readFloatVector;
        VECTOR_DECODERS[Type.DOUBLE.code] = Decoder::readDoubleVector;
        VECTOR_DECODERS[Type.STRING.code] = Decoder::readStringVectorAsBytes;
        VECTOR_DECODERS[Type.UUID.code] = Decoder::readUuidVector;
        VECTOR_DECODERS[Type.NULL.code] = Decoder::readNullVector;
        VECTOR_DECODERS[Type.VECTOR.code] = VariantReader::readVectorVector;
    }

    static {
        Arrays.setAll(SKIPPERS, idx -> decoder -> {
            throw new IllegalArgumentException("Unknown type with code " + String.valueOf(idx));
        });

        SKIPPERS[Type.CONTAINER.code] = VariantReader::skipContainer;
        SKIPPERS[Type.BYTE.code] = Decoder::skipByte;
        SKIPPERS[Type.SHORT.code] = Decoder::skipShort;
        SKIPPERS[Type.INTEGER.code] = Decoder::skipInteger;
        SKIPPERS[Type.LONG.code] = Decoder::skipLong;
        SKIPPERS[Type.FLAG.code] = Decoder::skipFlag;
        SKIPPERS[Type.FLOAT.code] = Decoder::skipFloat;
        SKIPPERS[Type.DOUBLE.code] = Decoder::skipDouble;
        SKIPPERS[Type.STRING.code] = Decoder::skipString;
        SKIPPERS[Type.UUID.code] = Decoder::skipUuid;
        SKIPPERS[Type.NULL.code] = Decoder::skipNull;
        SKIPPERS[Type.VECTOR.code] = VariantReader::skipVector;
    }

    static {
        Arrays.setAll(VECTOR_SKIPPERS, idx -> decoder -> {
            throw new IllegalArgumentException("Unknown type with code " + String.valueOf(idx));
        });

        VECTOR_SKIPPERS[Type.CONTAINER.code] = VariantReader::skipContainerVector;
        VECTOR_SKIPPERS[Type.BYTE.code] = Decoder::skipByteVector;
        VECTOR_SKIPPERS[Type.SHORT.code] = Decoder::skipShortVector;
        VECTOR_SKIPPERS[Type.INTEGER.code] = Decoder::skipIntegerVector;
        VECTOR_SKIPPERS[Type.LONG.code] = Decoder::skipLongVector;
        VECTOR_SKIPPERS[Type.FLAG.code] = Decoder::skipFlagVector;
        VECTOR_SKIPPERS[Type.FLOAT.code] = Decoder::skipFloatVector;
        VECTOR_SKIPPERS[Type.DOUBLE.code] = Decoder::skipDoubleVector;
        VECTOR_SKIPPERS[Type.STRING.code] = Decoder::skipStringVector;
        VECTOR_SKIPPERS[Type.UUID.code] = Decoder::skipUuidVector;
        VECTOR_SKIPPERS[Type.NULL.code] = Decoder::skipNullVector;
        VECTOR_SKIPPERS[Type.VECTOR.code] = VariantReader::skipVectorVector;
    }

    private static Type readType(Decoder decoder) {
        return Type.valueOf(decoder.readByte());
    }

    private static Object readValue(Decoder decoder, Type type) {
        return DECODERS[type.code].apply(decoder);
    }

    private static int skipValue(Decoder decoder, Type type) {
        return SKIPPERS[type.code].applyAsInt(decoder);
    }

    private static Vector readVector(Decoder decoder) {
        Type type = readType(decoder);
        return new Vector(type, VECTOR_DECODERS[type.code].apply(decoder));
    }

    private static int skipVector(Decoder decoder) {
        Type type = readType(decoder);
        return SizeOf.TYPE + VECTOR_SKIPPERS[type.code].applyAsInt(decoder);
    }

    private static Vector[] readVectorVector(Decoder decoder) {
        int length = decoder.readVectorLength();
        Vector[] vectors = new Vector[length];
        for (int i = 0; i < length; i++) {
            vectors[i] = readVector(decoder);
        }
        return vectors;
    }

    private static int skipVectorVector(Decoder decoder) {
        int position = decoder.position();

        int length = decoder.readVectorLength();
        while (length-- > 0) {
            skipVector(decoder);
        }

        return decoder.position() - position;
    }

    private static Container readContainer(Decoder decoder) {
        return CONTAINER_READER.read(decoder);
    }

    private static Object readContainerVector(Decoder decoder) {
        return CONTAINER_VECTOR_READER.read(decoder);
    }

    private static int skipContainer(Decoder decoder) {
        return CONTAINER_READER.skip(decoder);
    }

    private static int skipContainerVector(Decoder decoder) {
        return CONTAINER_VECTOR_READER.skip(decoder);
    }

    @Override
    public Variant read(Decoder decoder) {
        Type type = readType(decoder);
        Object value = readValue(decoder, type);
        return new Variant(type, value);
    }

    @Override
    public int skip(Decoder decoder) {
        int skipped = 0;
        Type type = readType(decoder);
        skipped += SizeOf.BYTE;
        skipped += skipValue(decoder, type);
        return skipped;
    }

    private interface ObjectReader extends Function<Decoder, Object> {
    }

    private interface ObjectSkipper extends ToIntFunction<Decoder> {
    }
}
