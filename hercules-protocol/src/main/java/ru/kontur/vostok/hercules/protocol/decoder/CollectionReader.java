package ru.kontur.vostok.hercules.protocol.decoder;

import java.lang.reflect.Array;

public abstract class CollectionReader<T> implements Reader<T[]> {
    private final Reader<T> elementReader;
    private final Class<T> clazz;

    public CollectionReader(Reader<T> elementReader, Class<T> clazz) {
        this.elementReader = elementReader;
        this.clazz = clazz;
    }

    protected abstract int readLength(Decoder decoder);

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
