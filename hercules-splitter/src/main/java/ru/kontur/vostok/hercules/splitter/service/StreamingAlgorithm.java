package ru.kontur.vostok.hercules.splitter.service;

import javax.annotation.concurrent.NotThreadSafe;

/**
 * Base interface of streaming algorithms.
 * <p>
 * Implementations of this interface cannot be thread safe by design because of inner state that cannot be shared.
 *
 * @author Aleksandr Yuferov
 */
@NotThreadSafe
public interface StreamingAlgorithm {

    /**
     * Update inner state.
     *
     * @param buffer Buffer with data.
     * @param offset Starting offset in buffer.
     * @param length Length of data to be processed.
     */
    void update(byte[] buffer, int offset, int length);

    /**
     * Update inner state.
     *
     * @param buffer Buffer with data, that should be all processed.
     */
    default void update(byte[] buffer) {
        update(buffer, 0, buffer.length);
    }

    /**
     * Reset the state of the algorithm to initial.
     */
    void reset();
}
