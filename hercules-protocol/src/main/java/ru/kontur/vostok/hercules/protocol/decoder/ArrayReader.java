package ru.kontur.vostok.hercules.protocol.decoder;

import java.lang.reflect.Array;

public class ArrayReader<T> extends CollectionReader<T> {

    public ArrayReader(Reader<T> elementReader, Class<T> clazz) {
        super(elementReader, clazz);
    }

    @Override
    protected int readLength(Decoder decoder) {
        return decoder.readArrayLength();
    }
}
