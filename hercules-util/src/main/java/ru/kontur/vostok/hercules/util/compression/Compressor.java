package ru.kontur.vostok.hercules.util.compression;

import java.nio.ByteBuffer;

/**
 * Compressor.
 *
 * @author Gregory Koshelev
 */
public interface Compressor {
    /**
     * The maximum compressed data length.
     *
     * @param length the source data length
     * @return the maximum compressed data length
     */
    int maxCompressedLength(int length);

    /**
     * Compress source data.
     *
     * @param src  the source data
     * @param dest the compressed data
     */
    void compress(ByteBuffer src, ByteBuffer dest);
}
