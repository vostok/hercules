package ru.kontur.vostok.hercules.protocol.encoder;

/**
 * Hercules Protocol Writer for collection
 * @param <T> Type of collection for which defined Writer<T>
 * @author jdk
 */
public abstract class CollectionWriter<T> implements Writer<T[]> {
    private final Writer<T> elementWriter;

    public CollectionWriter(Writer<T> elementWriter) {
        this.elementWriter = elementWriter;
    }

    /**
     * Write length of collection with encoder
     * @param encoder Encoder for write data
     * @param length Length of data which must be written
     */
    abstract void writeLength(Encoder encoder, int length);

    /**
     * Write containers' collection with encoder
     * @param encoder Encoder for write data
     * @param array Collection of containers which are must be written
     */
    @Override
    public void write(Encoder encoder, T[] array) {
        writeLength(encoder, array.length);
        for (T element : array) {
            elementWriter.write(encoder, element);
        }
    }
}
