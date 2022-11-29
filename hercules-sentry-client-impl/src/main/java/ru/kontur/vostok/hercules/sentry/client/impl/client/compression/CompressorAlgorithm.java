package ru.kontur.vostok.hercules.sentry.client.impl.client.compression;

import java.io.IOException;

/**
 * @author Aleksandr Yuferov
 */
public interface CompressorAlgorithm {

    String contentEncoding();

    byte[] compressData(byte[] data) throws IOException;
}
