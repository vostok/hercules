package ru.kontur.vostok.hercules.sentry.client.impl.client.compression;

import java.io.IOException;
import java.net.http.HttpRequest;
import java.util.function.IntPredicate;

/**
 * @author Aleksandr Yuferov
 */
public class Compressor {

    private final IntPredicate strategy;
    private final CompressorAlgorithm algorithm;

    public Compressor(IntPredicate strategy, CompressorAlgorithm algorithm) {
        this.strategy = strategy;
        this.algorithm = algorithm;
    }

    public byte[] processData(HttpRequest.Builder requestBuilder, byte[] data) throws IOException {
        if (strategy.test(data.length)) {
            requestBuilder.header("Content-Encoding", algorithm.contentEncoding());
            return algorithm.compressData(data);
        }
        return data;
    }
}
