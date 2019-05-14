package ru.kontur.vostok.hercules.util.collection;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * @author Gregory Koshelev
 */
public class CollectionUtil {
    @SuppressWarnings("rawtypes")
    public static final Deque EMPTY_DEQUE = new EmptyDeque<>();

    /**
     * Returns empty immutable deque.
     *
     * @param <T> type of elements
     * @return empty immutable deque instance
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public static <T> Deque<T> emptyDeque() {
        return (Deque<T>) EMPTY_DEQUE;
    }

    private CollectionUtil() {
        /* static class */
    }

    private static class EmptyDeque<E> implements Deque<E> {
        @Override
        public void addFirst(E e) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void addLast(E e) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean offerFirst(E e) {
            return false;
        }

        @Override
        public boolean offerLast(E e) {
            return false;
        }

        @Override
        public E removeFirst() {
            throw new NoSuchElementException();
        }

        @Override
        public E removeLast() {
            throw new NoSuchElementException();
        }

        @Nullable
        @Override
        public E pollFirst() {
            return null;
        }

        @Nullable
        @Override
        public E pollLast() {
            return null;
        }

        @Override
        public E getFirst() {
            throw new NoSuchElementException();
        }

        @Override
        public E getLast() {
            throw new NoSuchElementException();
        }

        @Override
        public E peekFirst() {
            return null;
        }

        @Override
        public E peekLast() {
            return null;
        }

        @Override
        public boolean removeFirstOccurrence(Object o) {
            return false;
        }

        @Override
        public boolean removeLastOccurrence(Object o) {
            return false;
        }

        @Override
        public boolean add(E e) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean offer(E e) {
            return false;
        }

        @Override
        public E remove() {
            throw new NoSuchElementException();
        }

        @Override
        public E poll() {
            return null;
        }

        @Override
        public E element() {
            throw new NoSuchElementException();
        }

        @Override
        public E peek() {
            return null;
        }

        @Override
        public void push(E e) {
            throw new UnsupportedOperationException();
        }

        @Override
        public E pop() {
            throw new NoSuchElementException();
        }

        @Override
        public boolean remove(Object o) {
            return false;
        }

        @Override
        public boolean containsAll(@NotNull Collection<?> c) {
            return c.isEmpty();
        }

        @Override
        public boolean addAll(@NotNull Collection<? extends E> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean removeAll(@NotNull Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean retainAll(@NotNull Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clear() {
        }

        @Override
        public boolean contains(Object o) {
            return false;
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @NotNull
        @Override
        public Iterator<E> iterator() {
            return Collections.emptyIterator();
        }

        @NotNull
        @Override
        public Object[] toArray() {
            return new Object[0];
        }

        @NotNull
        @Override
        public <T> T[] toArray(@NotNull T[] a) {
            if (a.length > 0)
                a[0] = null;
            return a;
        }

        @NotNull
        @Override
        public Iterator<E> descendingIterator() {
            return Collections.emptyIterator();
        }
    }
}
