package ru.kontur.vostok.hercules.protocol.decoder;

import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Variant;

import java.util.HashMap;
import java.util.Map;

public class ContainerReader implements Reader<Container> {

    private static final VariantReader variantReader = new VariantReader();

    @Override
    public Container read(Decoder decoder) {
        int length = decoder.readUnsignedByte();
        Map<String, Variant> variantMap = new HashMap<>(length);
        while (0 <= --length) {
            String fieldName = decoder.readString();
            Variant variant = variantReader.read(decoder);
            variantMap.put(fieldName, variant);
        }
        return new Container(variantMap);
    }
}
