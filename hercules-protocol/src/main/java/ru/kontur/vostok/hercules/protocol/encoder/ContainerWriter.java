package ru.kontur.vostok.hercules.protocol.encoder;

import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.protocol.VectorConstants;

import java.util.Map;

public class ContainerWriter implements Writer<Container> {

    private static final VariantWriter variantWriter = new VariantWriter();

    @Override
    public void write(Encoder encoder, Container value) {
        if (VectorConstants.VECTOR_LENGTH_EXCEEDED <= value.size() ) {
            throw new RuntimeException(VectorConstants.VECTOR_LENGTH_ERROR_MESSAGE);
        }

        encoder.writeByte((byte) value.size());
        for (Map.Entry<String, Variant> entry : value) {
            encoder.writeString(entry.getKey());
            variantWriter.write(encoder, entry.getValue());
        }
    }
}
