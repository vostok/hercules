package ru.kontur.vostok.hercules.sentry.client.impl.client.compression;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;

/**
 * @author Aleksandr Yuferov
 */
public class GzipCompressorAlgorithm implements CompressorAlgorithm {

    @Override
    public String contentEncoding() {
        return "gzip";
    }

    @Override
    public byte[] compressData(byte[] data) throws IOException {
        var compressed = new ByteArrayOutputStream(data.length);
        try (var stream = new GZIPOutputStream(compressed)) {
            stream.write(data);
        }
        return compressed.toByteArray();
    }
}
