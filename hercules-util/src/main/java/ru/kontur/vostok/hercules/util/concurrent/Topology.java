package ru.kontur.vostok.hercules.util.concurrent;

import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Gregory Koshelev
 */
public class Topology<T> implements Iterable<T> {
    private final ReentrantLock lock = new ReentrantLock();
    private final TopologyIterator<T> iterator = new TopologyIterator<>();
    private volatile State state;

    public Topology(T[] topology) {
        Object[] array = new Object[topology.length];
        System.arraycopy(topology, 0, array, 0, topology.length);
        this.state = new State(array);
    }

    @SuppressWarnings("unchecked")
    public T next() {
        return (T) state.next();
    }

    public boolean isEmpty() {
        return state.isEmpty();
    }

    public void add(T element) {
        lock.lock();
        try {
            Object[] array = state.array;
            int newSize = array.length + 1;
            Object[] newArray = new Object[newSize];
            System.arraycopy(array, 0, newArray, 0, array.length);
            newArray[newSize - 1] = element;
            state = new State(newArray);
        } finally {
            lock.unlock();
        }
    }

    public boolean remove(T element) {
        lock.lock();
        try {
            if (state.isEmpty()) {
                return false;
            }

            final Object[] array = state.array;
            int index = 0;
            while (index < array.length && !array[index].equals(element)) {
                index++;
            }

            if (index == array.length) {
                return false;
            }

            final int newSize = array.length - 1;
            Object[] newArray = new Object[array.length - 1];
            System.arraycopy(array, 0, newArray, 0, index);
            if (index != newSize) {
                System.arraycopy(array, index + 1, newArray, index, newSize - index);
            }

            state = new State(newArray);
            return true;
        } finally {
            lock.unlock();
        }
    }

    @NotNull
    @Override
    public Iterator<T> iterator() {
        return iterator;
    }

    private final class TopologyIterator<T> implements Iterator<T> {

        @Override
        public boolean hasNext() {
            return !state.isEmpty();
        }

        @SuppressWarnings("unchecked")
        @Override
        public T next() {
            return (T) state.next();
        }
    }

    private static final class State {
        private final Object[] array;

        private AtomicInteger it = new AtomicInteger(0);

        private State(Object[] array) {
            this.array = array;
        }

        public Object next() {
            if (array.length == 0) {
                throw new TopologyIsEmptyException("Topology is empty");
            }
            return array[(it.getAndIncrement() & 0x7FFFFFFF) % array.length];
        }

        public boolean isEmpty() {
            return array.length == 0;
        }

    }

    public static class TopologyIsEmptyException extends RuntimeException {
        public TopologyIsEmptyException() {
            super();
        }

        public TopologyIsEmptyException(String message) {
            super(message);
        }
    }
}
