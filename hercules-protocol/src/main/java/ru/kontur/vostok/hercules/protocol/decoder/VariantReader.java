package ru.kontur.vostok.hercules.protocol.decoder;

import ru.kontur.vostok.hercules.protocol.Type;
import ru.kontur.vostok.hercules.protocol.Variant;

import java.util.Arrays;
import java.util.function.Function;
import java.util.function.ToIntFunction;

public class VariantReader implements Reader<Variant> {

    @Override
    public Variant read(Decoder decoder) {
        Type type = readType(decoder);
        Object value = readValue(decoder, type);
        return new Variant(type, value);
    }

    @Override
    public void skip(Decoder decoder) {
        Type type = readType(decoder);
        skipValue(decoder, type);
    }

    private static Type readType(Decoder decoder) {
        return Type.valueOf(decoder.readByte());
    }

    private static Object readValue(Decoder decoder, Type type) {
        return decoders[type.value].apply(decoder);
    }

    private static int skipValue(Decoder decoder, Type type) {
        return skippers[type.value].applyAsInt(decoder);
    }

    /**
     * Type decoders
     */
    @SuppressWarnings("unchecked")
    private final static Function<Decoder, Object>[] decoders = new Function[256];
    static {
        Arrays.setAll(decoders, idx -> decoder -> {
            throw new IllegalArgumentException("Unknown type with code " + String.valueOf(idx));
        });

        decoders[Type.BYTE.value] = Decoder::readByte;
        decoders[Type.SHORT.value] = Decoder::readShort;
        decoders[Type.INTEGER.value] = Decoder::readInteger;
        decoders[Type.LONG.value] = Decoder::readLong;
        decoders[Type.FLAG.value] = Decoder::readFlag;
        decoders[Type.FLOAT.value] = Decoder::readFloat;
        decoders[Type.DOUBLE.value] = Decoder::readDouble;
        decoders[Type.STRING.value] = Decoder::readStringAsBytes;
        decoders[Type.TEXT.value] = Decoder::readTextAsBytes;

        decoders[Type.BYTE_VECTOR.value] = Decoder::readByteVector;
        decoders[Type.SHORT_VECTOR.value] = Decoder::readShortVector;
        decoders[Type.INTEGER_VECTOR.value] = Decoder::readIntegerVector;
        decoders[Type.LONG_VECTOR.value] = Decoder::readLongVector;
        decoders[Type.FLAG_VECTOR.value] = Decoder::readFlagVector;
        decoders[Type.FLOAT_VECTOR.value] = Decoder::readFloatVector;
        decoders[Type.DOUBLE_VECTOR.value] = Decoder::readDoubleVector;
        decoders[Type.STRING_VECTOR.value] = Decoder::readStringVectorAsBytes;
        decoders[Type.TEXT_VECTOR.value] = Decoder::readTextVectorAsBytes;

        decoders[Type.BYTE_ARRAY.value] = Decoder::readByteArray;
        decoders[Type.SHORT_ARRAY.value] = Decoder::readShortArray;
        decoders[Type.INTEGER_ARRAY.value] = Decoder::readIntegerArray;
        decoders[Type.LONG_ARRAY.value] = Decoder::readLongArray;
        decoders[Type.FLAG_ARRAY.value] = Decoder::readFlagArray;
        decoders[Type.FLOAT_ARRAY.value] = Decoder::readFloatArray;
        decoders[Type.DOUBLE_ARRAY.value] = Decoder::readDoubleArray;
        decoders[Type.STRING_ARRAY.value] = Decoder::readStringArrayAsBytes;
        decoders[Type.TEXT_ARRAY.value] = Decoder::readTextArrayAsBytes;
    }

    /**
     * Skip methods
     */
    @SuppressWarnings("unchecked")
    private final static ToIntFunction<Decoder>[] skippers = new ToIntFunction[256];
    static {
        Arrays.setAll(skippers, idx -> decoder -> {
            throw new IllegalArgumentException("Unknown type with code " + String.valueOf(idx));
        });

        skippers[Type.BYTE.value] = Decoder::skipByte;
        skippers[Type.SHORT.value] = Decoder::skipShort;
        skippers[Type.INTEGER.value] = Decoder::skipInteger;
        skippers[Type.LONG.value] = Decoder::skipLong;
        skippers[Type.FLAG.value] = Decoder::skipFlag;
        skippers[Type.FLOAT.value] = Decoder::skipFloat;
        skippers[Type.DOUBLE.value] = Decoder::skipDouble;
        skippers[Type.STRING.value] = Decoder::skipString;
        skippers[Type.TEXT.value] = Decoder::skipText;

        skippers[Type.BYTE_VECTOR.value] = Decoder::skipByteVector;
        skippers[Type.SHORT_VECTOR.value] = Decoder::skipShortVector;
        skippers[Type.INTEGER_VECTOR.value] = Decoder::skipIntegerVector;
        skippers[Type.LONG_VECTOR.value] = Decoder::skipLongVector;
        skippers[Type.FLAG_VECTOR.value] = Decoder::skipFlagVector;
        skippers[Type.FLOAT_VECTOR.value] = Decoder::skipFloatVector;
        skippers[Type.DOUBLE_VECTOR.value] = Decoder::skipDoubleVector;
        skippers[Type.STRING_VECTOR.value] = Decoder::skipStringVector;
        skippers[Type.TEXT_VECTOR.value] = Decoder::skipTextVector;

        skippers[Type.BYTE_ARRAY.value] = Decoder::skipByteArray;
        skippers[Type.SHORT_ARRAY.value] = Decoder::skipShortArray;
        skippers[Type.INTEGER_ARRAY.value] = Decoder::skipIntegerArray;
        skippers[Type.LONG_ARRAY.value] = Decoder::skipLongArray;
        skippers[Type.FLAG_ARRAY.value] = Decoder::skipFlagArray;
        skippers[Type.FLOAT_ARRAY.value] = Decoder::skipFloatArray;
        skippers[Type.DOUBLE_ARRAY.value] = Decoder::skipDoubleArray;
        skippers[Type.STRING_ARRAY.value] = Decoder::skipStringArray;
        skippers[Type.TEXT_ARRAY.value] = Decoder::skipTextArray;
    }
}
