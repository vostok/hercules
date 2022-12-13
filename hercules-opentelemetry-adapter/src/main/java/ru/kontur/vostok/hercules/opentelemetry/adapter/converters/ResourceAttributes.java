package ru.kontur.vostok.hercules.opentelemetry.adapter.converters;

/**
 * Resource attributes
 *
 * @author Innokentiy Krivonosov
 */
public class ResourceAttributes {
    /**
     * @see <a href="https://github.com/open-telemetry/opentelemetry-specification/blob/main/specification/resource/semantic_conventions/README.md">
     * * Resource Semantic Conventions</a>
     */
    public static final String SERVICE_NAME = "service.name";
    public static final String SERVICE_INSTANCE_ID = "service.instance.id";
    public static final String HOST_NAME = "host.name";

    /**
     * Custom attributes for metrics only
     */
    public static final String METRIC_RESOURCE_PREFIX = "metric_";
}
