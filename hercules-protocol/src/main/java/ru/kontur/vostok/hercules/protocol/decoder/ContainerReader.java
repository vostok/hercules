package ru.kontur.vostok.hercules.protocol.decoder;

import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Variant;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class ContainerReader implements Reader<Container> {

    private static final VariantReader variantReader = new VariantReader();

    private final Set<String> fields;

    private ContainerReader(Set<String> fields) {
        this.fields = fields;
    }

    @Override
    public Container read(Decoder decoder) {
        short length = decoder.readShort();
        Map<String, Variant> variantMap = new HashMap<>(length);
        while (0 <= --length) {
            String fieldName = decoder.readString();
            if (Objects.isNull(fields) || fields.contains(fieldName)) {
                Variant variant = variantReader.read(decoder);
                variantMap.put(fieldName, variant);
            }
            else {
                variantReader.skip(decoder);
            }
        }
        return new Container(variantMap);
    }

    @Override
    public void skip(Decoder decoder) {
        short length = decoder.readShort();
        while (0 <= --length) {
            decoder.skipString();
            variantReader.skip(decoder);
        }
    }

    public static ContainerReader readAllFields() {
        return new ContainerReader(null);
    }

    public static ContainerReader readFields(Set<String> fields) {
        return new ContainerReader(fields);
    }
}
