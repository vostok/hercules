package ru.kontur.vostok.hercules.elastic.sink;

import java.io.IOException;

/**
 * Elastic client adapter.
 *
 * @author Innokentiy Krivonosov
 */
public interface ElasticClient {
    /**
     * Index given data.
     * <p/>
     * Expected data in ElasticSearch bulk request format. If compressionGzipEnable has value {@code true} then header
     * {@link ru.kontur.vostok.hercules.http.header.HttpHeaders#CONTENT_ENCODING} will be added with 'gzip' value
     *
     * @param dataToIndex           data to index.
     * @param compressionGzipEnable data is compressed.
     * @return Result of index request.
     */
    ElasticResponseHandler.Result index(byte[] dataToIndex, boolean compressionGzipEnable);

    /**
     * Try to ping cluster.
     *
     * @return {@code true} if cluster answers the test request or {@code false} if request finished with error.
     */
    boolean ping();

    /**
     * @param data data to index.
     * @return compressed data
     * @throws IOException IO exception
     */
    byte[] compressData(byte[] data) throws IOException;
}
