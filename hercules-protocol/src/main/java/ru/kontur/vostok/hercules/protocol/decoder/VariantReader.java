package ru.kontur.vostok.hercules.protocol.decoder;

import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Type;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.protocol.Vector;

import java.util.Arrays;
import java.util.function.Function;
import java.util.function.ToIntFunction;

public class VariantReader implements Reader<Variant> {

    public static final VariantReader INSTANCE = new VariantReader();
    private static final ContainerReader CONTAINER_READER = ContainerReader.INSTANCE;
    private static final ContainerVectorReader CONTAINER_VECTOR_READER = ContainerVectorReader.INSTANCE;

    private static final ObjectReader[] TYPE_DECODERS = new ObjectReader[256];
    private static final ObjectReader[] VECTOR_OF_TYPE_DECODERS = new ObjectReader[256];
    private static final ObjectSkipper[] TYPE_SKIPPERS = new ObjectSkipper[256];
    private static final ObjectSkipper[] VECTOR_OF_TYPE_SKIPPERS = new ObjectSkipper[256];

    static {
        Arrays.setAll(TYPE_DECODERS, idx -> decoder -> {
            throw new IllegalArgumentException("Unknown type with code " + String.valueOf(idx));
        });

        TYPE_DECODERS[Type.CONTAINER.code] = VariantReader::readContainer;
        TYPE_DECODERS[Type.BYTE.code] = Decoder::readByte;
        TYPE_DECODERS[Type.SHORT.code] = Decoder::readShort;
        TYPE_DECODERS[Type.INTEGER.code] = Decoder::readInteger;
        TYPE_DECODERS[Type.LONG.code] = Decoder::readLong;
        TYPE_DECODERS[Type.FLAG.code] = Decoder::readFlag;
        TYPE_DECODERS[Type.FLOAT.code] = Decoder::readFloat;
        TYPE_DECODERS[Type.DOUBLE.code] = Decoder::readDouble;
        TYPE_DECODERS[Type.STRING.code] = Decoder::readStringAsBytes;
        TYPE_DECODERS[Type.UUID.code] = Decoder::readUuid;
        TYPE_DECODERS[Type.NULL.code] = Decoder::readNull;
        TYPE_DECODERS[Type.VECTOR.code] = VariantReader::readVector;
    }

    static {
        Arrays.setAll(VECTOR_OF_TYPE_DECODERS, idx -> decoder -> {
            throw new IllegalArgumentException("Unknown type with code " + String.valueOf(idx));
        });

        VECTOR_OF_TYPE_DECODERS[Type.CONTAINER.code] = VariantReader::readContainerVector;
        VECTOR_OF_TYPE_DECODERS[Type.BYTE.code] = Decoder::readByteVector;
        VECTOR_OF_TYPE_DECODERS[Type.SHORT.code] = Decoder::readShortVector;
        VECTOR_OF_TYPE_DECODERS[Type.INTEGER.code] = Decoder::readIntegerVector;
        VECTOR_OF_TYPE_DECODERS[Type.LONG.code] = Decoder::readLongVector;
        VECTOR_OF_TYPE_DECODERS[Type.FLAG.code] = Decoder::readFlagVector;
        VECTOR_OF_TYPE_DECODERS[Type.FLOAT.code] = Decoder::readFloatVector;
        VECTOR_OF_TYPE_DECODERS[Type.DOUBLE.code] = Decoder::readDoubleVector;
        VECTOR_OF_TYPE_DECODERS[Type.STRING.code] = Decoder::readStringVectorAsBytes;
        VECTOR_OF_TYPE_DECODERS[Type.UUID.code] = Decoder::readUuidVector;
        VECTOR_OF_TYPE_DECODERS[Type.NULL.code] = Decoder::readNullVector;
        VECTOR_OF_TYPE_DECODERS[Type.VECTOR.code] = VariantReader::readVectorVector;
    }

    static {
        Arrays.setAll(TYPE_SKIPPERS, idx -> decoder -> {
            throw new IllegalArgumentException("Unknown type with code " + String.valueOf(idx));
        });

        TYPE_SKIPPERS[Type.CONTAINER.code] = VariantReader::skipContainer;
        TYPE_SKIPPERS[Type.BYTE.code] = Decoder::skipByte;
        TYPE_SKIPPERS[Type.SHORT.code] = Decoder::skipShort;
        TYPE_SKIPPERS[Type.INTEGER.code] = Decoder::skipInteger;
        TYPE_SKIPPERS[Type.LONG.code] = Decoder::skipLong;
        TYPE_SKIPPERS[Type.FLAG.code] = Decoder::skipFlag;
        TYPE_SKIPPERS[Type.FLOAT.code] = Decoder::skipFloat;
        TYPE_SKIPPERS[Type.DOUBLE.code] = Decoder::skipDouble;
        TYPE_SKIPPERS[Type.STRING.code] = Decoder::skipString;
        TYPE_SKIPPERS[Type.UUID.code] = Decoder::skipUuid;
        TYPE_SKIPPERS[Type.NULL.code] = Decoder::skipNull;
        TYPE_SKIPPERS[Type.VECTOR.code] = VariantReader::skipVector;
    }

    static {
        Arrays.setAll(VECTOR_OF_TYPE_SKIPPERS, idx -> decoder -> {
            throw new IllegalArgumentException("Unknown type with code " + String.valueOf(idx));
        });

        VECTOR_OF_TYPE_SKIPPERS[Type.CONTAINER.code] = VariantReader::skipContainerVector;
        VECTOR_OF_TYPE_SKIPPERS[Type.BYTE.code] = Decoder::skipByteVector;
        VECTOR_OF_TYPE_SKIPPERS[Type.SHORT.code] = Decoder::skipShortVector;
        VECTOR_OF_TYPE_SKIPPERS[Type.INTEGER.code] = Decoder::skipIntegerVector;
        VECTOR_OF_TYPE_SKIPPERS[Type.LONG.code] = Decoder::skipLongVector;
        VECTOR_OF_TYPE_SKIPPERS[Type.FLAG.code] = Decoder::skipFlagVector;
        VECTOR_OF_TYPE_SKIPPERS[Type.FLOAT.code] = Decoder::skipFloatVector;
        VECTOR_OF_TYPE_SKIPPERS[Type.DOUBLE.code] = Decoder::skipDoubleVector;
        VECTOR_OF_TYPE_SKIPPERS[Type.STRING.code] = Decoder::skipStringVector;
        VECTOR_OF_TYPE_SKIPPERS[Type.UUID.code] = Decoder::skipUuidVector;
        VECTOR_OF_TYPE_SKIPPERS[Type.NULL.code] = Decoder::skipNullVector;
        VECTOR_OF_TYPE_SKIPPERS[Type.VECTOR.code] = VariantReader::skipVectorVector;
    }

    private static Type readType(Decoder decoder) {
        return Type.valueOf(decoder.readByte());
    }

    private static Object readValue(Decoder decoder, Type type) {
        return TYPE_DECODERS[type.code].apply(decoder);
    }

    private static int skipValue(Decoder decoder, Type type) {
        return TYPE_SKIPPERS[type.code].applyAsInt(decoder);
    }

    private static Vector readVector(Decoder decoder) {
        Type type = readType(decoder);
        return new Vector(type, VECTOR_OF_TYPE_DECODERS[type.code].apply(decoder));
    }

    private static int skipVector(Decoder decoder) {
        Type type = readType(decoder);
        return SizeOf.TYPE + VECTOR_OF_TYPE_SKIPPERS[type.code].applyAsInt(decoder);
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
