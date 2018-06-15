package ru.kontur.vostok.hercules.protocol.decoder;

import ru.kontur.vostok.hercules.protocol.Type;
import ru.kontur.vostok.hercules.protocol.Variant;

public class VariantReader {

    public static Variant read(Decoder decoder) {
        Type type = decoder.readType();
        Object value = decoder.read(type);
        return new Variant(type, value);
    }
}
