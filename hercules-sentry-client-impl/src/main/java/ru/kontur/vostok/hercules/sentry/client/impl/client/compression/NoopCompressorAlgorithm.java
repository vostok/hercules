package ru.kontur.vostok.hercules.sentry.client.impl.client.compression;

/**
 * @author Aleksandr Yuferov
 */
public class NoopCompressorAlgorithm implements CompressorAlgorithm {

    @Override
    public String contentEncoding() {
        return null;
    }

    @Override
    public byte[] compressData(byte[] data) {
        return data;
    }
}
