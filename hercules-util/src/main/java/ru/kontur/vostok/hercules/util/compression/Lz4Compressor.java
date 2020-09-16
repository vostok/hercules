package ru.kontur.vostok.hercules.util.compression;

import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4Factory;

import java.nio.ByteBuffer;

/**
 * LZ4 compressor implementation.
 *
 * @author Gregory Koshelev
 */
public final class Lz4Compressor implements Compressor {
    private final LZ4Compressor compressor = LZ4Factory.fastestInstance().fastCompressor();

    @Override
    public int maxCompressedLength(int length) {
        return compressor.maxCompressedLength(length);
    }

    @Override
    public void compress(ByteBuffer src, ByteBuffer dest) {
        compressor.compress(src, dest);
        dest.flip();
    }
}
