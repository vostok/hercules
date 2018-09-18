package ru.kontur.vostok.hercules.protocol.decoder;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * ReaderIterator<T> uses Reader<T> to read array of elements. Not thread safe.
 * @param <T>
 */
public class ReaderIterator<T> implements Iterator<T> {

    private final Decoder decoder;
    private final Reader<T> elementReader;
    private int total;
    private int remaining;
    private boolean initialReadPerformed = false;

    public ReaderIterator(Decoder decoder, Reader<T> elementReader) {
        this.decoder = decoder;
        this.elementReader = elementReader;
    }

    @Override
    public boolean hasNext() {
        initialRead();
        return remaining > 0;
    }

    @Override
    public T next() {
        initialRead();
        if (remaining <= 0) {
            throw new NoSuchElementException();
        }

        T result = elementReader.read(decoder);
        --remaining;
        return result;
    }

    public int getTotal() {
        initialRead();
        return total;
    }

    private void initialRead() {
        if (!initialReadPerformed) {
            this.total = decoder.readInteger();
            this.remaining = total;
            this.initialReadPerformed = true;
        }
    }
}
