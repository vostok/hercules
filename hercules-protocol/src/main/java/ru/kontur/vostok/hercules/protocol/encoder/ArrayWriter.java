package ru.kontur.vostok.hercules.protocol.encoder;

public class ArrayWriter<T> implements Writer<T[]> {

    private final Writer<T> elementWriter;

    public ArrayWriter(Writer<T> elementWriter) {
        this.elementWriter = elementWriter;
    }

    @Override
    public void write(Encoder encoder, T[] array) {
        encoder.writeInteger(array.length);
        for (T element : array) {
            elementWriter.write(encoder, element);
        }
    }
}
