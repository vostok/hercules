package ru.kontur.vostok.hercules.opentelemetry.adapter.converters;

import io.opentelemetry.proto.resource.v1.Resource;
import io.opentelemetry.proto.trace.v1.Span;
import io.opentelemetry.proto.trace.v1.Status;
import org.junit.Test;
import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.hpath.HPath;

import static org.junit.Assert.assertEquals;
import static ru.kontur.vostok.hercules.opentelemetry.adapter.converters.TraceConverterTest.getLongValueAttr;
import static ru.kontur.vostok.hercules.opentelemetry.adapter.converters.TraceConverterTest.getStringValueAttr;

/**
 * @author Innokentiy Krivonosov
 */
public class TraceAnnotationsConverterTest {

    @Test
    public void httpRequestClient() {
        Span span = Span.newBuilder()
                .setKind(Span.SpanKind.SPAN_KIND_CLIENT)
                .setName("GET test.com")
                .setStatus(Status.newBuilder().setCode(Status.StatusCode.STATUS_CODE_OK).build())
                .addAttributes(getStringValueAttr("http.flavor", "2.0"))
                .addAttributes(getStringValueAttr("http.method", "GET"))
                .addAttributes(getStringValueAttr("http.url", "https://test.com"))
                .addAttributes(getLongValueAttr("http.request_content_length", 1000))
                .addAttributes(getLongValueAttr("http.status_code", 200))
                .addAttributes(getLongValueAttr("http.response_content_length", 2000))
                .addAttributes(getLongValueAttr("thread.id", 1))
                .addAttributes(getStringValueAttr("thread.name", "main"))
                .addAttributes(getStringValueAttr("net.transport", "ip_tcp"))
                .addAttributes(getLongValueAttr("net.peer.port", 443))
                .addAttributes(getStringValueAttr("http.request.targetEnvironment", "default"))
                .addAttributes(getStringValueAttr("http.request.targetService", "Portal.Fias.Api"))
                .build();

        Resource resource = Resource.newBuilder()
                .addAttributes(getStringValueAttr("service.name", "test-app"))
                .addAttributes(getStringValueAttr("host.name", "localhost"))
                .addAttributes(getStringValueAttr("component", "test-app"))
                .addAttributes(getStringValueAttr("environment", "default"))
                .addAttributes(getStringValueAttr(ResourceAttributes.METRIC_RESOURCE_PREFIX + "environment", "metric-env"))
                .build();

        Container annotations = TraceAnnotationsConverter.getAnnotations(span, resource);
        assertEquals(19, annotations.count());
        assertEquals("(STRING) http-request-client", HPath.fromPath("kind").extract(annotations).toString());
        assertEquals("(STRING) GET test.com", HPath.fromPath("operation").extract(annotations).toString());
        assertEquals("(STRING) success", HPath.fromPath("status").extract(annotations).toString());

        assertEquals("(STRING) test-app", HPath.fromPath("application").extract(annotations).toString());
        assertEquals("(STRING) localhost", HPath.fromPath("host").extract(annotations).toString());
        assertEquals("(STRING) test-app", HPath.fromPath("component").extract(annotations).toString());
        assertEquals("(STRING) default", HPath.fromPath("environment").extract(annotations).toString());

        assertEquals("(STRING) GET", HPath.fromPath("http.request.method").extract(annotations).toString());
        assertEquals("(STRING) https://test.com", HPath.fromPath("http.request.url").extract(annotations).toString());
        assertEquals("(LONG) 1000", HPath.fromPath("http.request.size").extract(annotations).toString());
        assertEquals("(INTEGER) 200", HPath.fromPath("http.response.code").extract(annotations).toString());
        assertEquals("(LONG) 2000", HPath.fromPath("http.response.size").extract(annotations).toString());

        assertEquals("(STRING) default", HPath.fromPath("http.request.targetEnvironment").extract(annotations).toString());
        assertEquals("(STRING) Portal.Fias.Api", HPath.fromPath("http.request.targetService").extract(annotations).toString());

        assertEquals("(STRING) 2.0", HPath.fromPath("http.flavor").extract(annotations).toString());
        assertEquals("(STRING) ip_tcp", HPath.fromPath("net.transport").extract(annotations).toString());
        assertEquals("(LONG) 1", HPath.fromPath("thread.id").extract(annotations).toString());
        assertEquals("(STRING) main", HPath.fromPath("thread.name").extract(annotations).toString());
        assertEquals("(LONG) 443", HPath.fromPath("net.peer.port").extract(annotations).toString());
    }

    @Test
    public void httpClusterClient() {
        Span span = Span.newBuilder()
                .setKind(Span.SpanKind.SPAN_KIND_CLIENT)
                .addAttributes(getStringValueAttr("http.cluster.strategy", "sequential"))
                .addAttributes(getStringValueAttr("http.cluster.status", "success"))
                .build();

        Container annotations = TraceAnnotationsConverter.getAnnotations(span, Resource.newBuilder().build());
        assertEquals(3, annotations.count());
        assertEquals("(STRING) http-request-client", HPath.fromPath("kind").extract(annotations).toString());

        assertEquals("(STRING) sequential", HPath.fromPath("http.cluster.strategy").extract(annotations).toString());
        assertEquals("(STRING) success", HPath.fromPath("http.cluster.status").extract(annotations).toString());
    }

    @Test
    public void errorServerSpan() {
        Span span = Span.newBuilder()
                .setKind(Span.SpanKind.SPAN_KIND_SERVER)
                .setStatus(Status.newBuilder().setCode(Status.StatusCode.STATUS_CODE_ERROR).build())
                .addAttributes(getStringValueAttr("http.target", "/users/123"))
                .addAttributes(getStringValueAttr("net.peer.name", "test name"))
                .addAttributes(getStringValueAttr("net.peer.ip", "0.0.0.0"))
                .build();

        Container annotations = TraceAnnotationsConverter.getAnnotations(span, Resource.newBuilder().build());
        assertEquals(5, annotations.count());
        assertEquals("(STRING) http-request-server", HPath.fromPath("kind").extract(annotations).toString());
        assertEquals("(STRING) error", HPath.fromPath("status").extract(annotations).toString());
        assertEquals("(STRING) /users/123", HPath.fromPath("http.request.url").extract(annotations).toString());

        assertEquals("(STRING) test name", HPath.fromPath("http.client.name").extract(annotations).toString());
        assertEquals("(STRING) 0.0.0.0", HPath.fromPath("http.client.address").extract(annotations).toString());
    }

    @Test
    public void supportTextWarningStatusAndKind() {
        Span span = Span.newBuilder()
                .addAttributes(getStringValueAttr("status", "warning"))
                .addAttributes(getStringValueAttr("kind", "custom-request-client"))
                .build();

        Container annotations = TraceAnnotationsConverter.getAnnotations(span, Resource.newBuilder().build());
        assertEquals(2, annotations.count());
        assertEquals("(STRING) custom-request-client", HPath.fromPath("kind").extract(annotations).toString());
        assertEquals("(STRING) warning", HPath.fromPath("status").extract(annotations).toString());
    }

    @Test
    public void rabbitmqTest() {
        Span span = Span.newBuilder()
                .setKind(Span.SpanKind.SPAN_KIND_PRODUCER)
                .setStatus(Status.newBuilder().setCode(Status.StatusCode.STATUS_CODE_OK).build())
                .addAttributes(getStringValueAttr("net.peer.name", "ms"))
                .addAttributes(getStringValueAttr("net.peer.ip", "1234"))
                .addAttributes(getStringValueAttr("messaging.system", "rabbitmq"))
                .addAttributes(getStringValueAttr("messaging.destination", "T"))
                .addAttributes(getStringValueAttr("messaging.destination_kind", "topic"))
                .addAttributes(getStringValueAttr("messaging.operation", "process"))
                .addAttributes(getStringValueAttr("messaging.message_id", "a1"))
                .build();

        Container annotations = TraceAnnotationsConverter.getAnnotations(span, Resource.newBuilder().build());
        assertEquals(8, annotations.count());
        assertEquals("(STRING) success", HPath.fromPath("status").extract(annotations).toString());

        assertEquals("(STRING) ms", HPath.fromPath("http.client.name").extract(annotations).toString());
        assertEquals("(STRING) 1234", HPath.fromPath("http.client.address").extract(annotations).toString());

        assertEquals("(STRING) rabbitmq", HPath.fromPath("messaging.system").extract(annotations).toString());
        assertEquals("(STRING) T", HPath.fromPath("messaging.destination").extract(annotations).toString());
        assertEquals("(STRING) topic", HPath.fromPath("messaging.destination_kind").extract(annotations).toString());
        assertEquals("(STRING) process", HPath.fromPath("messaging.operation").extract(annotations).toString());
        assertEquals("(STRING) a1", HPath.fromPath("messaging.message_id").extract(annotations).toString());
    }
}