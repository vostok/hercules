package ru.kontur.vostok.hercules.protocol.decoder;

import java.lang.reflect.Array;

public class ArrayReader<T> implements Reader<T[]> {

    private final Reader<T> elementReader;
    private final Class<T> clazz;

    public ArrayReader(Reader<T> elementReader, Class<T> clazz) {
        this.elementReader = elementReader;
        this.clazz = clazz;
    }

    @Override
    public T[] read(Decoder decoder) {
        int count = decoder.readInteger();
        T[] result = (T[]) Array.newInstance(clazz, count);
        for (int i = 0; i < count; ++i) {
            result[i] = elementReader.read(decoder);
        }
        return result;
    }
}
