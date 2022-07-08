package ru.kontur.vostok.hercules.opentelemetry.adapter.converters;

/**
 * Vostok tracing annotations
 *
 * @author Innokentiy Krivonosov
 * @see <a href="https://github.com/vostok/tracing.abstractions/blob/master/README.md">Vostok tracing</a>
 */
public class VostokAnnotations {

    /**
     * Common annotations
     */
    protected static final String KIND = "kind";
    protected static final String OPERATION = "operation";
    protected static final String STATUS = "status";
    protected static final String APPLICATION = "application";
    protected static final String ENVIRONMENT = "environment";
    protected static final String HOST = "host";
    protected static final String COMPONENT = "component";

    /**
     * HTTP requests
     */
    protected static final String HTTP_REQUEST_METHOD = "http.request.method";
    protected static final String HTTP_REQUEST_URL = "http.request.url";
    protected static final String HTTP_REQUEST_SIZE = "http.request.size";
    protected static final String HTTP_RESPONSE_CODE = "http.response.code";
    protected static final String HTTP_RESPONSE_SIZE = "http.response.size";

    /**
     * HTTP client (direct)
     */
    protected static final String HTTP_REQUEST_TARGET_SERVICE = "http.request.targetService";
    protected static final String HTTP_REQUEST_TARGET_ENVIRONMENT = "http.request.targetEnvironment";

    /**
     * HTTP client (cluster)
     */
    protected static final String HTTP_CLUSTER_STRATEGY = "http.cluster.strategy";
    protected static final String HTTP_CLUSTER_STATUS = "http.cluster.status";

    /**
     * HTTP server
     */
    protected static final String HTTP_CLIENT_NAME = "http.client.name";
    protected static final String HTTP_CLIENT_ADDRESS = "http.client.address";
}
