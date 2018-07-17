package ru.kontur.vostok.hercules.protocol.encoder;

/**
 * Hercules Protocol Writer for array
 * @param <T> Type of collection for which defined Writer<T>
 * @author jdk
 */
public class ArrayWriter<T> extends CollectionWriter<T> {
    public ArrayWriter(Writer<T> elementWriter) {
        super(elementWriter);
    }

    @Override
    protected void writeLength(Encoder encoder, int length) {
        encoder.writeInteger(length);
    }
}
