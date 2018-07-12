package ru.kontur.vostok.hercules.protocol.encoder;

import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Variant;

import java.util.Map;

public class ContainerWriter implements Writer<Container> {

    private static final VariantWriter variantWriter = new VariantWriter();

    @Override
    public void write(Encoder encoder, Container value) {
        if (Short.MAX_VALUE < value.size()) {
            throw new RuntimeException("Only " + Short.MAX_VALUE + " fields are supported");
        }

        encoder.writeShort((short) value.size());
        for (Map.Entry<String, Variant> entry : value) {
            encoder.writeString(entry.getKey());
            variantWriter.write(encoder, entry.getValue());
        }
    }
}
