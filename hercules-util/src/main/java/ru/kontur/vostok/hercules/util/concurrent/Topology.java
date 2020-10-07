package ru.kontur.vostok.hercules.util.concurrent;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Topology is a list of elements which is iterated infinitely while topology is not empty.
 * <p>
 * Topology can be thread-safely iterated since modification through {@link #add(T)} or {@link #remove(T)} is thread-safe too.
 *
 * @author Gregory Koshelev
 */
public class Topology<T> implements Iterable<T> {
    private final ReentrantLock lock = new ReentrantLock();
    private final TopologyIterator iterator = new TopologyIterator();
    private volatile State state;

    public Topology(T[] topology) {
        Object[] array = new Object[topology.length];
        System.arraycopy(topology, 0, array, 0, topology.length);
        this.state = initiateState(array);
    }

    /**
     * Iterate to the next element in the topology list. The first element is following by the last one.
     *
     * @return the next element in the topology list
     * @throws TopologyIsEmptyException if the topology is empty
     */
    @SuppressWarnings("unchecked")
    public T next() throws TopologyIsEmptyException {
        return (T) state.next();
    }

    /**
     * Returns {@code true} if topology is empty.
     *
     * @return {@code true} if topology is empty
     */
    public boolean isEmpty() {
        return state.isEmpty();
    }

    /**
     * Add the element to the topology list.
     *
     * @param element the element to add
     */
    public void add(T element) {
        lock.lock();
        try {
            Object[] array = state.array;
            int newSize = array.length + 1;
            Object[] newArray = new Object[newSize];
            System.arraycopy(array, 0, newArray, 0, array.length);
            newArray[newSize - 1] = element;
            state = initiateState(newArray);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Remove the first occurrence of the element in the topology list.
     *
     * @param element the element to remove
     * @return {@code true} if the element was removed from the topology list, otherwise return {@code false}
     */
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
            Object[] newArray = new Object[newSize];
            System.arraycopy(array, 0, newArray, 0, index);
            if (index != newSize) {
                System.arraycopy(array, index + 1, newArray, index, newSize - index);
            }

            state = initiateState(newArray);
            return true;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns current size of the topology list.
     *
     * @return current size of the topology list
     */
    public int size() {
        return state.array.length;
    }

    @NotNull
    @Override
    public Iterator<T> iterator() {
        return iterator;
    }

    @SuppressWarnings("unchecked")
    public List<T> asList() {
        return (List<T>) Arrays.asList(state.array);
    }

    /**
     * Initiate a state of the topology.
     * <p>
     * Inheritors should override to provide an another topology iteration algorithm.
     *
     * @param array an object array holds topology elements
     * @return the state
     */
    protected State initiateState(Object[] array) {
        return new GlobalState(array);
    }

    private final class TopologyIterator implements Iterator<T> {

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

    protected static abstract class State {
        private final Object[] array;

        protected State(Object[] array) {
            this.array = array;
        }

        public Object next() throws TopologyIsEmptyException {
            if (array.length == 0) {
                throw new TopologyIsEmptyException("Topology is empty");
            }
            return array[(seed() & 0x7FFFFFFF) % array.length];
        }

        public boolean isEmpty() {
            return array.length == 0;
        }

        /**
         * Return a value which determines next element in the topology.
         *
         * @return a value which determines next element in the topology
         */
        protected abstract int seed();
    }

    private static final class GlobalState extends State {
        // Start an iteration with a random element when the topology changes.
        // It provides the better balancing across the topology.
        private final AtomicInteger it = new AtomicInteger(ThreadLocalRandom.current().nextInt());

        private GlobalState(Object[] array) {
            super(array);
        }

        @Override
        protected int seed() {
            return it.getAndIncrement();
        }
    }

    /**
     * {@link TopologyIsEmptyException} is thrown when try to get element from the empty topology.
     */
    public static class TopologyIsEmptyException extends RuntimeException {
        public TopologyIsEmptyException() {
            super();
        }

        public TopologyIsEmptyException(String message) {
            super(message);
        }
    }
}
