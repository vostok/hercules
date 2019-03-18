package ru.kontur.vostok.hercules.protocol.decoder;

import ru.kontur.vostok.hercules.protocol.decoder.exceptions.InvalidDataException;

import java.nio.BufferUnderflowException;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class ReaderIterator<T> implements Iterator<T> {

    private final Decoder decoder;
    private final Reader<T> elementReader;
    private final int total;
    private int remaining;

    public ReaderIterator(Decoder decoder, Reader<T> elementReader) throws InvalidDataException {
        this.decoder = decoder;
        this.elementReader = elementReader;
        try {
            this.total = decoder.readInteger();
        } catch (BufferUnderflowException e) {
            throw new InvalidDataException(e);
        }
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
