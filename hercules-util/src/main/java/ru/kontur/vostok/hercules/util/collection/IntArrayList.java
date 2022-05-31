package ru.kontur.vostok.hercules.util.collection;

import java.util.Arrays;
import java.util.Objects;

/**
 * Int-primitive array list implementation.
 *
 * @author Aleksandr Yuferov
 */
public class IntArrayList {
    private static final int[] EMPTY_DATA = new int[0];
    private static final int FACTOR = 2;
    private static final int DEFAULT_SIZE = 4;

    private int[] data;
    private int size = 0;

    public IntArrayList() {
        this(0);
    }

    public IntArrayList(int capacity) {
        data = capacity == 0 ? EMPTY_DATA : new int[capacity];
    }

    public void add(int element) {
        if (data.length == size) {
            int newSize = data.length != 0 ? data.length * FACTOR : DEFAULT_SIZE;
            data = Arrays.copyOf(data, newSize);
        }
        data[size++] = element;
    }

    public int get(int index) {
        Objects.checkIndex(index, size);
        return data[index];
    }

    public void set(int index, int value) {
        Objects.checkIndex(index, size);
        data[index] = value;
    }

    public int size() {
        return size;
    }

    public int capacity() {
        return data.length;
    }

    public void clean() {
        size = 0;
    }
}
