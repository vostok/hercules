package ru.kontur.vostok.hercules.opentelemetry.adapter.converters;

import io.opentelemetry.proto.resource.v1.Resource;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static ru.kontur.vostok.hercules.opentelemetry.adapter.converters.TraceConverterTest.getLongValueAttr;
import static ru.kontur.vostok.hercules.opentelemetry.adapter.converters.TraceConverterTest.getStringValueAttr;

public class ServiceNameExtractorTest {

    @Test
    public void testGetServiceName() {
        Resource resource = Resource.newBuilder()
                .addAttributes(getStringValueAttr("service.name", "test-app"))
                .build();

        assertEquals("test-app", ServiceNameExtractor.getServiceName(resource));
    }

    @Test
    public void wrongType() {
        Resource resource = Resource.newBuilder()
                .addAttributes(getLongValueAttr("service.name", 1))
                .build();

        assertEquals("unknown", ServiceNameExtractor.getServiceName(resource));
    }

    @Test
    public void unknown() {
        Resource resource = Resource.newBuilder().build();
        assertEquals("unknown", ServiceNameExtractor.getServiceName(resource));
    }
}