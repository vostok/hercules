package ru.kontur.vostok.hercules.protocol.decoder;

import java.lang.reflect.Array;

/**
 * Hercules Protocol Reader for collection
 * @param <T> type of collection
 */
public abstract class CollectionReader<T> implements Reader<T[]> {
    private final Reader<T> elementReader;
    private final Class<T> clazz;

    public CollectionReader(Reader<T> elementReader, Class<T> clazz) {
        this.elementReader = elementReader;
        this.clazz = clazz;
    }

    /**
     * Read length of collection
     * @param decoder Decoder for read data
     * @return length of collection
     */
    protected abstract int readLength(Decoder decoder);

    /**
     * Read containers' collection with decoder
     * @param decoder Decoder for read data
     * @return collection of containers
     */
    @Override
    public T[] read(Decoder decoder) {
        int count = readLength(decoder);
        T[] result = (T[]) Array.newInstance(clazz, count);
        for (int i = 0; i < count; ++i) {
            result[i] = elementReader.read(decoder);
        }
        return result;
    }
}
