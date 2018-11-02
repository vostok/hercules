package ru.kontur.vostok.hercules.util;

/**
 * @author Gregory Koshelev
 */
public class Maps {
    /**
     * Default loadFactor for HashMap
     */
    public static final float HASH_MAP_DEFAULT_LOAD_FACTOR = 0.75f;

    /**
     * HashMap's maximum capacity
     */
    public static final int HASH_MAP_MAXIMUM_CAPACITY = 1 << 30;

    public static int effectiveHashMapCapacity(int size) {
        return effectiveHashMapCapacity(size, HASH_MAP_DEFAULT_LOAD_FACTOR);
    }

    public static int effectiveHashMapCapacity(int size, float loadFactor) {
        float threshold = (size / loadFactor) + 1.0f;
        return (threshold < (float) HASH_MAP_MAXIMUM_CAPACITY)
                ? (int) threshold
                : HASH_MAP_MAXIMUM_CAPACITY;
    }
}
