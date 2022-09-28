package ru.kontur.vostok.hercules.opentelemetry.adapter.converters;

import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.resource.v1.Resource;
import io.opentelemetry.proto.trace.v1.Span;
import io.opentelemetry.proto.trace.v1.Status;
import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.TinyString;
import ru.kontur.vostok.hercules.protocol.Variant;

import java.util.Map;

/**
 * Trace annotations converter is used to convert OpenTelemetry span to Hercules trace annotations Container.
 *
 * @author Innokentiy Krivonosov
 * @see <a href="https://github.com/open-telemetry/opentelemetry-specification/blob/main/specification/trace/semantic_conventions/README.md">
 * Trace Semantic Conventions</a>
 * @see <a href="https://github.com/open-telemetry/opentelemetry-proto/blob/main/opentelemetry/proto/trace/v1/trace.proto">
 * OpenTelemetry trace.proto</a>
 */
public class TraceAnnotationsConverter {

    protected static Container getAnnotations(Span span, Resource resource) {
        Container.ContainerBuilder builder = Container.builder();

        Variant kind = getVostokName(span.getKind());
        if (kind != null) {
            builder.tag(VostokAnnotations.KIND, kind);
        }

        if (!span.getName().isEmpty()) {
            builder.tag(VostokAnnotations.OPERATION, Variant.ofString(span.getName()));
        }

        if (!span.getTraceState().isEmpty()) {
            builder.tag(TRACE_STATE, Variant.ofString(span.getTraceState()));
        }

        Status status = span.getStatus();
        Variant vostokStatus = getVostokStatus(status);
        if (vostokStatus != null) {
            builder.tag(VostokAnnotations.STATUS, vostokStatus);
        }

        if (status.getCode() == Status.StatusCode.STATUS_CODE_ERROR && !status.getMessage().isEmpty()) {
            builder.tag(ERROR, Variant.ofString(status.getMessage()));
        }

        for (KeyValue keyValue : span.getAttributesList()) {
            if (HTTP_STATUS_CODE.equals(keyValue.getKey())) {
                builder.tag(VostokAnnotations.HTTP_RESPONSE_CODE, getIntValue(keyValue.getValue()));
            } else {
                TinyString vostokName = SPAN_CONVERT_MAP.get(keyValue.getKey());
                if (vostokName != null) {
                    builder.tag(vostokName, VariantConverter.convert(keyValue.getValue()));
                } else {
                    builder.tag(keyValue.getKey(), VariantConverter.convert(keyValue.getValue()));
                }
            }
        }

        for (KeyValue keyValue : resource.getAttributesList()) {
            TinyString vostokName = RESOURCE_CONVERT_MAP.get(keyValue.getKey());
            if (vostokName != null) {
                builder.tag(vostokName, VariantConverter.convert(keyValue.getValue()));
            } else {
                builder.tag(keyValue.getKey(), VariantConverter.convert(keyValue.getValue()));
            }
        }

        return builder.build();
    }

    private static Variant getIntValue(AnyValue anyValue) {
        return Variant.ofInteger(Math.toIntExact(anyValue.getIntValue()));
    }

    private static Variant getVostokName(Span.SpanKind kind) {
        switch (kind) {
            case SPAN_KIND_CLIENT:
                return Variant.ofString("http-request-client");
            case SPAN_KIND_SERVER:
                return Variant.ofString("http-request-server");
            default:
                return null;
        }
    }

    private static Variant getVostokStatus(Status status) {
        switch (status.getCode()) {
            case STATUS_CODE_OK:
                return Variant.ofString("success");
            case STATUS_CODE_ERROR:
                return Variant.ofString("error");
            default:
                return null;
        }
    }

    private static final String SERVICE_NAME = "service.name";
    private static final String HOST_NAME = "host.name";

    private static final String HTTP_METHOD = "http.method";
    private static final String HTTP_URL = "http.url";
    private static final String HTTP_TARGET = "http.target";
    private static final String HTTP_REQUEST_CONTENT_LENGTH = "http.request_content_length";
    private static final String HTTP_STATUS_CODE = "http.status_code";
    private static final String HTTP_RESPONSE_CONTENT_LENGTH = "http.response_content_length";

    private static final String NET_PEER_NAME = "net.peer.name";
    private static final String NET_PEER_IP = "net.peer.ip";

    private static final TinyString ERROR = TinyString.of("error");
    private static final TinyString TRACE_STATE = TinyString.of("tracestate");

    private static final Map<String, TinyString> SPAN_CONVERT_MAP = Map.ofEntries(
            Map.entry(HTTP_METHOD, VostokAnnotations.HTTP_REQUEST_METHOD),
            Map.entry(HTTP_URL, VostokAnnotations.HTTP_REQUEST_URL),
            Map.entry(HTTP_TARGET, VostokAnnotations.HTTP_REQUEST_URL),
            Map.entry(HTTP_REQUEST_CONTENT_LENGTH, VostokAnnotations.HTTP_REQUEST_SIZE),
            Map.entry(HTTP_RESPONSE_CONTENT_LENGTH, VostokAnnotations.HTTP_RESPONSE_SIZE),

            Map.entry(NET_PEER_NAME, VostokAnnotations.HTTP_CLIENT_NAME),
            Map.entry(NET_PEER_IP, VostokAnnotations.HTTP_CLIENT_ADDRESS)
    );

    private static final Map<String, TinyString> RESOURCE_CONVERT_MAP = Map.ofEntries(
            Map.entry(SERVICE_NAME, VostokAnnotations.APPLICATION),
            Map.entry(HOST_NAME, VostokAnnotations.HOST)
    );
}
