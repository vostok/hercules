package ru.kontur.vostok.hercules.protocol.encoder;

public abstract class CollectionWriter<T> implements Writer<T[]> {
    private final Writer<T> elementWriter;

    public CollectionWriter(Writer<T> elementWriter) {
        this.elementWriter = elementWriter;
    }

    abstract void writeLength(Encoder encoder, int length);

    @Override
    public void write(Encoder encoder, T[] array) {
        writeLength(encoder, array.length);
        for (T element : array) {
            elementWriter.write(encoder, element);
        }
    }
}
