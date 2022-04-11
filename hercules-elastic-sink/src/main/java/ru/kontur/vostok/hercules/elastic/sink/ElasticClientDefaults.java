package ru.kontur.vostok.hercules.elastic.sink;

import org.apache.http.client.config.RequestConfig;
import org.elasticsearch.client.RestClientBuilder;

/**
 * @author Gregory Koshelev
 */
public final class ElasticClientDefaults {
    public static final int DEFAULT_MAX_CONNECTIONS = RestClientBuilder.DEFAULT_MAX_CONN_TOTAL;

    public static final int DEFAULT_MAX_CONNECTIONS_PER_ROUTE = RestClientBuilder.DEFAULT_MAX_CONN_PER_ROUTE;

    public static final int DEFAULT_RETRY_TIMEOUT_MS = RestClientBuilder.DEFAULT_MAX_RETRY_TIMEOUT_MILLIS;

    public static final int DEFAULT_CONNECTION_TIMEOUT_MS = RestClientBuilder.DEFAULT_CONNECT_TIMEOUT_MILLIS;

    /**
     * The default timeout in milliseconds used when retrieve a connection instance from http-client.
     * See {@link RequestConfig#getConnectionRequestTimeout()} for details.
     */
    public static final int DEFAULT_CONNECTION_REQUEST_TIMEOUT_MS = 500;

    public static final int DEFAULT_SOCKET_TIMEOUT_MS = RestClientBuilder.DEFAULT_SOCKET_TIMEOUT_MILLIS;

    public static final String[] DEFAULT_REDEFINED_EXCEPTIONS = new String[0];

    /**
     * Elastic client will not try to create index if missing.
     */
    public static final boolean DEFAULT_INDEX_CREATION_ENABLE = false;

    /**
     * Elastic client will send requests without compression.
     */
    public static final boolean DEFAULT_COMPRESSION_GZIP_ENABLE = false;

    private ElasticClientDefaults() {
        /* static class */
    }
}
