package ru.kontur.vostok.hercules.protocol.decoder;

import ru.kontur.vostok.hercules.protocol.Type;
import ru.kontur.vostok.hercules.protocol.Variant;

public class VariantReader implements Reader<Variant> {

    @Override
    public Variant read(Decoder decoder) {
        Type type = decoder.readType();
        Object value = decoder.read(type);
        return new Variant(type, value);
    }

    @Override
    public void skip(Decoder decoder) {
        Type type = decoder.readType();
        decoder.skip(type);
    }
}
