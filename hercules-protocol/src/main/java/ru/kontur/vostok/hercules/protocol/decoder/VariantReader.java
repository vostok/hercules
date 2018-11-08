package ru.kontur.vostok.hercules.protocol.decoder;

import ru.kontur.vostok.hercules.protocol.Type;
import ru.kontur.vostok.hercules.protocol.Variant;

import java.util.Arrays;
import java.util.function.Function;
import java.util.function.ToIntFunction;

public class VariantReader implements Reader<Variant> {

    public static final VariantReader INSTANCE = new VariantReader();
    private static final ContainerReader CONTAINER_READER = ContainerReader.INSTANCE;
    private static final ContainerArrayReader CONTAINER_ARRAY_READER = ContainerArrayReader.INSTANCE;
    private static final ContainerVectorReader CONTAINER_VECTOR_READER = ContainerVectorReader.INSTANCE;
    /**
     * Type decoders
     */
    private final static ObjectReader[] DECODERS = new ObjectReader[256];
    /**
     * Skip methods
     */
    private final static ObjectSkipper[] SKIPPERS = new ObjectSkipper[256];

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
        DECODERS[Type.TEXT.code] = Decoder::readTextAsBytes;

        DECODERS[Type.CONTAINER_VECTOR.code] = VariantReader::readContainerVector;
        DECODERS[Type.BYTE_VECTOR.code] = Decoder::readByteVector;
        DECODERS[Type.SHORT_VECTOR.code] = Decoder::readShortVector;
        DECODERS[Type.INTEGER_VECTOR.code] = Decoder::readIntegerVector;
        DECODERS[Type.LONG_VECTOR.code] = Decoder::readLongVector;
        DECODERS[Type.FLAG_VECTOR.code] = Decoder::readFlagVector;
        DECODERS[Type.FLOAT_VECTOR.code] = Decoder::readFloatVector;
        DECODERS[Type.DOUBLE_VECTOR.code] = Decoder::readDoubleVector;
        DECODERS[Type.STRING_VECTOR.code] = Decoder::readStringVectorAsBytes;
        DECODERS[Type.TEXT_VECTOR.code] = Decoder::readTextVectorAsBytes;

        DECODERS[Type.CONTAINER_ARRAY.code] = VariantReader::readContainerArray;
        DECODERS[Type.BYTE_ARRAY.code] = Decoder::readByteArray;
        DECODERS[Type.SHORT_ARRAY.code] = Decoder::readShortArray;
        DECODERS[Type.INTEGER_ARRAY.code] = Decoder::readIntegerArray;
        DECODERS[Type.LONG_ARRAY.code] = Decoder::readLongArray;
        DECODERS[Type.FLAG_ARRAY.code] = Decoder::readFlagArray;
        DECODERS[Type.FLOAT_ARRAY.code] = Decoder::readFloatArray;
        DECODERS[Type.DOUBLE_ARRAY.code] = Decoder::readDoubleArray;
        DECODERS[Type.STRING_ARRAY.code] = Decoder::readStringArrayAsBytes;
        DECODERS[Type.TEXT_ARRAY.code] = Decoder::readTextArrayAsBytes;
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
        SKIPPERS[Type.TEXT.code] = Decoder::skipText;

        SKIPPERS[Type.CONTAINER_VECTOR.code] = VariantReader::skipContainerVector;
        SKIPPERS[Type.BYTE_VECTOR.code] = Decoder::skipByteVector;
        SKIPPERS[Type.SHORT_VECTOR.code] = Decoder::skipShortVector;
        SKIPPERS[Type.INTEGER_VECTOR.code] = Decoder::skipIntegerVector;
        SKIPPERS[Type.LONG_VECTOR.code] = Decoder::skipLongVector;
        SKIPPERS[Type.FLAG_VECTOR.code] = Decoder::skipFlagVector;
        SKIPPERS[Type.FLOAT_VECTOR.code] = Decoder::skipFloatVector;
        SKIPPERS[Type.DOUBLE_VECTOR.code] = Decoder::skipDoubleVector;
        SKIPPERS[Type.STRING_VECTOR.code] = Decoder::skipStringVector;
        SKIPPERS[Type.TEXT_VECTOR.code] = Decoder::skipTextVector;

        SKIPPERS[Type.CONTAINER_ARRAY.code] = VariantReader::skipContainerArray;
        SKIPPERS[Type.BYTE_ARRAY.code] = Decoder::skipByteArray;
        SKIPPERS[Type.SHORT_ARRAY.code] = Decoder::skipShortArray;
        SKIPPERS[Type.INTEGER_ARRAY.code] = Decoder::skipIntegerArray;
        SKIPPERS[Type.LONG_ARRAY.code] = Decoder::skipLongArray;
        SKIPPERS[Type.FLAG_ARRAY.code] = Decoder::skipFlagArray;
        SKIPPERS[Type.FLOAT_ARRAY.code] = Decoder::skipFloatArray;
        SKIPPERS[Type.DOUBLE_ARRAY.code] = Decoder::skipDoubleArray;
        SKIPPERS[Type.STRING_ARRAY.code] = Decoder::skipStringArray;
        SKIPPERS[Type.TEXT_ARRAY.code] = Decoder::skipTextArray;
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

    private static Object readContainer(Decoder decoder) {
        return CONTAINER_READER.read(decoder);
    }

    private static Object readContainerArray(Decoder decoder) {
        return CONTAINER_ARRAY_READER.read(decoder);
    }

    private static Object readContainerVector(Decoder decoder) {
        return CONTAINER_VECTOR_READER.read(decoder);
    }

    private static int skipContainer(Decoder decoder) {
        return CONTAINER_READER.skip(decoder);
    }

    private static int skipContainerArray(Decoder decoder) {
        return CONTAINER_ARRAY_READER.skip(decoder);
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
