package ru.kontur.vostok.hercules.protocol.encoder;

import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Variant;

import java.util.Map;

public class ContainerWriter implements Writer<Container> {

    public static final ContainerWriter INSTANCE = new ContainerWriter();

    private static final VariantWriter VARIANT_WRITER = VariantWriter.INSTANCE;

    @Override
    public void write(Encoder encoder, Container value) {
        if (Short.MAX_VALUE < value.size()) {
            throw new RuntimeException("Only " + Short.MAX_VALUE + " tags are supported");
        }

        encoder.writeShort((short) value.size());
        for (Map.Entry<String, Variant> entry : value) {
            encoder.writeString(entry.getKey());
            VARIANT_WRITER.write(encoder, entry.getValue());
        }
    }
}
