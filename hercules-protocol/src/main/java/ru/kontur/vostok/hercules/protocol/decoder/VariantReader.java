package ru.kontur.vostok.hercules.protocol.decoder;

import ru.kontur.vostok.hercules.protocol.Type;
import ru.kontur.vostok.hercules.protocol.Variant;

import java.util.Arrays;
import java.util.function.Function;
import java.util.function.ToIntFunction;

public class VariantReader implements Reader<Variant> {

    public static final VariantReader INSTANCE = new VariantReader();
    private static final ContainerReader containerReader = ContainerReader.INSTANCE;
    private static final ContainerArrayReader containerArrayReader = ContainerArrayReader.INSTANCE;
    private static final ContainerVectorReader containerVectorReader = ContainerVectorReader.INSTANCE;
    /**
     * Type decoders
     */
    private final static ObjectReader[] decoders = new ObjectReader[256];
    /**
     * Skip methods
     */
    private final static ObjectSkipper[] skippers = new ObjectSkipper[256];

    static {
        Arrays.setAll(decoders, idx -> decoder -> {
            throw new IllegalArgumentException("Unknown type with code " + String.valueOf(idx));
        });

        decoders[Type.CONTAINER.code] = VariantReader::readContainer;
        decoders[Type.BYTE.code] = Decoder::readByte;
        decoders[Type.SHORT.code] = Decoder::readShort;
        decoders[Type.INTEGER.code] = Decoder::readInteger;
        decoders[Type.LONG.code] = Decoder::readLong;
        decoders[Type.FLAG.code] = Decoder::readFlag;
        decoders[Type.FLOAT.code] = Decoder::readFloat;
        decoders[Type.DOUBLE.code] = Decoder::readDouble;
        decoders[Type.STRING.code] = Decoder::readStringAsBytes;
        decoders[Type.TEXT.code] = Decoder::readTextAsBytes;

        decoders[Type.CONTAINER_VECTOR.code] = VariantReader::readContainerVector;
        decoders[Type.BYTE_VECTOR.code] = Decoder::readByteVector;
        decoders[Type.SHORT_VECTOR.code] = Decoder::readShortVector;
        decoders[Type.INTEGER_VECTOR.code] = Decoder::readIntegerVector;
        decoders[Type.LONG_VECTOR.code] = Decoder::readLongVector;
        decoders[Type.FLAG_VECTOR.code] = Decoder::readFlagVector;
        decoders[Type.FLOAT_VECTOR.code] = Decoder::readFloatVector;
        decoders[Type.DOUBLE_VECTOR.code] = Decoder::readDoubleVector;
        decoders[Type.STRING_VECTOR.code] = Decoder::readStringVectorAsBytes;
        decoders[Type.TEXT_VECTOR.code] = Decoder::readTextVectorAsBytes;

        decoders[Type.CONTAINER_ARRAY.code] = VariantReader::readContainerArray;
        decoders[Type.BYTE_ARRAY.code] = Decoder::readByteArray;
        decoders[Type.SHORT_ARRAY.code] = Decoder::readShortArray;
        decoders[Type.INTEGER_ARRAY.code] = Decoder::readIntegerArray;
        decoders[Type.LONG_ARRAY.code] = Decoder::readLongArray;
        decoders[Type.FLAG_ARRAY.code] = Decoder::readFlagArray;
        decoders[Type.FLOAT_ARRAY.code] = Decoder::readFloatArray;
        decoders[Type.DOUBLE_ARRAY.code] = Decoder::readDoubleArray;
        decoders[Type.STRING_ARRAY.code] = Decoder::readStringArrayAsBytes;
        decoders[Type.TEXT_ARRAY.code] = Decoder::readTextArrayAsBytes;
    }

    static {
        Arrays.setAll(skippers, idx -> decoder -> {
            throw new IllegalArgumentException("Unknown type with code " + String.valueOf(idx));
        });

        skippers[Type.CONTAINER.code] = VariantReader::skipContainer;
        skippers[Type.BYTE.code] = Decoder::skipByte;
        skippers[Type.SHORT.code] = Decoder::skipShort;
        skippers[Type.INTEGER.code] = Decoder::skipInteger;
        skippers[Type.LONG.code] = Decoder::skipLong;
        skippers[Type.FLAG.code] = Decoder::skipFlag;
        skippers[Type.FLOAT.code] = Decoder::skipFloat;
        skippers[Type.DOUBLE.code] = Decoder::skipDouble;
        skippers[Type.STRING.code] = Decoder::skipString;
        skippers[Type.TEXT.code] = Decoder::skipText;

        skippers[Type.CONTAINER_VECTOR.code] = VariantReader::skipContainerVector;
        skippers[Type.BYTE_VECTOR.code] = Decoder::skipByteVector;
        skippers[Type.SHORT_VECTOR.code] = Decoder::skipShortVector;
        skippers[Type.INTEGER_VECTOR.code] = Decoder::skipIntegerVector;
        skippers[Type.LONG_VECTOR.code] = Decoder::skipLongVector;
        skippers[Type.FLAG_VECTOR.code] = Decoder::skipFlagVector;
        skippers[Type.FLOAT_VECTOR.code] = Decoder::skipFloatVector;
        skippers[Type.DOUBLE_VECTOR.code] = Decoder::skipDoubleVector;
        skippers[Type.STRING_VECTOR.code] = Decoder::skipStringVector;
        skippers[Type.TEXT_VECTOR.code] = Decoder::skipTextVector;

        skippers[Type.CONTAINER_ARRAY.code] = VariantReader::skipContainerArray;
        skippers[Type.BYTE_ARRAY.code] = Decoder::skipByteArray;
        skippers[Type.SHORT_ARRAY.code] = Decoder::skipShortArray;
        skippers[Type.INTEGER_ARRAY.code] = Decoder::skipIntegerArray;
        skippers[Type.LONG_ARRAY.code] = Decoder::skipLongArray;
        skippers[Type.FLAG_ARRAY.code] = Decoder::skipFlagArray;
        skippers[Type.FLOAT_ARRAY.code] = Decoder::skipFloatArray;
        skippers[Type.DOUBLE_ARRAY.code] = Decoder::skipDoubleArray;
        skippers[Type.STRING_ARRAY.code] = Decoder::skipStringArray;
        skippers[Type.TEXT_ARRAY.code] = Decoder::skipTextArray;
    }

    private static Type readType(Decoder decoder) {
        return Type.valueOf(decoder.readByte());
    }

    private static Object readValue(Decoder decoder, Type type) {
        return decoders[type.code].apply(decoder);
    }

    private static int skipValue(Decoder decoder, Type type) {
        return skippers[type.code].applyAsInt(decoder);
    }

    private static Object readContainer(Decoder decoder) {
        return containerReader.read(decoder);
    }

    private static Object readContainerArray(Decoder decoder) {
        return containerArrayReader.read(decoder);
    }

    private static Object readContainerVector(Decoder decoder) {
        return containerVectorReader.read(decoder);
    }

    private static int skipContainer(Decoder decoder) {
        return containerReader.skip(decoder);
    }

    private static int skipContainerArray(Decoder decoder) {
        return containerArrayReader.skip(decoder);
    }

    private static int skipContainerVector(Decoder decoder) {
        return containerVectorReader.skip(decoder);
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