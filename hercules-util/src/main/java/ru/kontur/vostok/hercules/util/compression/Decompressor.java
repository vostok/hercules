package ru.kontur.vostok.hercules.util.compression;

import java.nio.ByteBuffer;

/**
 * @author Gregory Koshelev
 */
public interface Decompressor {
    void decompress(ByteBuffer src, ByteBuffer dest);

    default void decompress(byte[] src, ByteBuffer dest) {
        decompress(ByteBuffer.wrap(src), dest);
    }
}
