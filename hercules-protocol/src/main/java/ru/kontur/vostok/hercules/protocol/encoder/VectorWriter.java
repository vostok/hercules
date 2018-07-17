package ru.kontur.vostok.hercules.protocol.encoder;

import ru.kontur.vostok.hercules.protocol.VectorConstants;

public class VectorWriter<T> extends CollectionWriter<T>{
    public VectorWriter(Writer<T> elementWriter) {
        super(elementWriter);
    }

    @Override
    void writeLength(Encoder encoder, int length) {
        if (length >= VectorConstants.VECTOR_LENGTH_EXCEEDED) {
            throw new IllegalStateException(VectorConstants.VECTOR_LENGTH_ERROR_MESSAGE);
        }

        encoder.writeUnsignedByte(length);
    }
}
