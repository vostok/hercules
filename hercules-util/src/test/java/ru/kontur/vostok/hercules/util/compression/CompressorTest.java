package ru.kontur.vostok.hercules.util.compression;

import org.junit.Assert;
import org.junit.Test;

import java.nio.ByteBuffer;

/**
 * @author Gregory Koshelev
 */
public class CompressorTest {
    @Test
    public void shouldCompressDecompressLz4() {
        byte[] data = new byte[]{'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k'};
        Compressor compressor = new Lz4Compressor();
        ByteBuffer src = ByteBuffer.wrap(data);
        ByteBuffer dest = ByteBuffer.allocate(compressor.maxCompressedLength(data.length));
        compressor.compress(src, dest);
        src.flip();

        Decompressor decompressor = new Lz4Decompressor();
        ByteBuffer result = ByteBuffer.allocate(data.length);
        decompressor.decompress(dest, result);

        Assert.assertEquals(src, result);
    }
}
