package ru.kontur.vostok.hercules.util.compression;

import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4FastDecompressor;
import net.jpountz.lz4.LZ4SafeDecompressor;

import java.nio.ByteBuffer;

/**
 * @author Gregory Koshelev
 */
public class Lz4Decompressor implements Decompressor {
    private final LZ4FastDecompressor decompressor = LZ4Factory.fastestInstance().fastDecompressor();

    @Override
    public void decompress(ByteBuffer src, ByteBuffer dest) {
        decompressor.decompress(src, dest);
        dest.flip();
    }

}
