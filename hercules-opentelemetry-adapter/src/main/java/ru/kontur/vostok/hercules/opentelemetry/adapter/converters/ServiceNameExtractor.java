package ru.kontur.vostok.hercules.opentelemetry.adapter.converters;

import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.resource.v1.Resource;

/**
 * Extract <code>service.name</code> is required attribute in OpenTelemetry
 *
 * @author Innokentiy Krivonosov
 * @see <a href="https://github.com/open-telemetry/opentelemetry-specification/blob/main/specification/resource/semantic_conventions/README.md">
 * Resource Semantic Conventions</a>
 */
public class ServiceNameExtractor {

    public static String getServiceName(Resource resource) {
        for (KeyValue keyValue : resource.getAttributesList()) {
            if (ResourceAttributes.SERVICE_NAME.equals(keyValue.getKey()) && keyValue.getValue().hasStringValue()) {
                return keyValue.getValue().getStringValue();
            }
        }

        return "unknown";
    }
}
