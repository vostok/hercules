package ru.kontur.vostok.hercules.protocol.encoder;

import java.util.Collection;

/**
 * Hercules Protocol collection writer
 *
 * @param <T> type of collection element
 * @author Daniil Zhenikhov
 * @author Gregory Koshelev
 */
public class CollectionWriter<T> implements Writer<Collection<T>> {
    private final Writer<T> elementWriter;

    public CollectionWriter(Writer<T> elementWriter) {
        this.elementWriter = elementWriter;
    }

    /**
     * Write collection of {@code T} using encoder.
     *
     * @param encoder    encoder
     * @param collection collection
     */
    @Override
    public void write(Encoder encoder, Collection<T> collection) {
        encoder.writeInteger(collection.size());

        for (T element : collection) {
            elementWriter.write(encoder, element);
        }
    }
}
