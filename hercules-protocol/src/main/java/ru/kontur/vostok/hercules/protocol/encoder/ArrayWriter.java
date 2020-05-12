package ru.kontur.vostok.hercules.protocol.encoder;

/**
 * Hercules Protocol array writer
 *
 * @param <T> type of array element
 * @author Daniil Zhenikhov
 * @author Gregory Koshelev
 */
public class ArrayWriter<T> implements Writer<T[]> {
    private final Writer<T> elementWriter;

    public ArrayWriter(Writer<T> elementWriter) {
        this.elementWriter = elementWriter;
    }

    /**
     * Write array of {@code T} using encoder.
     *
     * @param encoder encoder
     * @param array   array
     */
    @Override
    public void write(Encoder encoder, T[] array) {
        encoder.writeInteger(array.length);

        for (T element : array) {
            elementWriter.write(encoder, element);
        }
    }

}
