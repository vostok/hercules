package ru.kontur.vostok.hercules.protocol.decoder;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class ReaderIterator<T> implements Iterator<T> {

    private final Decoder decoder;
    private final Reader<T> elementReader;
    private final int total;
    private int remaining;

    public ReaderIterator(Decoder decoder, Reader<T> elementReader) {
        this.decoder = decoder;
        this.elementReader = elementReader;
        this.total = decoder.readInteger();
        this.remaining = total;
    }

    @Override
    public boolean hasNext() {
        return remaining > 0;
    }

    @Override
    public T next() {
        if (remaining <= 0) {
            throw new NoSuchElementException();
        }

        T result = elementReader.read(decoder);
        --remaining;
        return result;
    }

    public int getTotal() {
        return total;
    }
}
