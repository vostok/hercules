package ru.kontur.vostok.hercules.splitter.service;

import javax.annotation.concurrent.NotThreadSafe;

/**
 * Interface of streaming hash functions.
 *
 * @author Aleksandr Yuferov
 */
@NotThreadSafe
public interface StreamingHasher extends StreamingAlgorithm {
    /**
     * Calculate result hash.
     *
     * @return Result of hash function.
     */
    int hash();
}
