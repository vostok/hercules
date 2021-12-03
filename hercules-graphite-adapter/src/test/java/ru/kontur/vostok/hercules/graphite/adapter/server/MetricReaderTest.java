package ru.kontur.vostok.hercules.graphite.adapter.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;
import ru.kontur.vostok.hercules.graphite.adapter.metric.Metric;
import ru.kontur.vostok.hercules.graphite.adapter.metric.MetricTag;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Gregory Koshelev
 */
public class MetricReaderTest {
    @Test
    public void shouldReadValidMetricWithoutTags() {
        ByteBuf buf = bufferFrom("metric.name 42 1637856786");
        Metric metric = MetricReader.read(buf);
        assertNotNull(metric);

        assertArrayEquals("metric.name".getBytes(StandardCharsets.UTF_8), metric.name());
        assertFalse(metric.hasTags());
        assertEquals(42d, metric.value(), 0.000000001);
        assertEquals(1637856786L, metric.timestamp());
    }

    @Test
    public void shouldReadValidMetricWithTags() {
        ByteBuf buf = bufferFrom("metric.name;tag=value 42 1637856786");
        Metric metric = MetricReader.read(buf);
        assertNotNull(metric);

        assertArrayEquals("metric.name".getBytes(StandardCharsets.UTF_8), metric.name());
        assertTrue(metric.hasTags());
        MetricTag[] tags = metric.tags();
        assertEquals(1, tags.length);
        assertArrayEquals("tag".getBytes(StandardCharsets.UTF_8), tags[0].key());
        assertArrayEquals("value".getBytes(StandardCharsets.UTF_8), tags[0].value());
        assertEquals(42d, metric.value(), 0.000000001);
        assertEquals(1637856786L, metric.timestamp());
    }

    @Test
    public void shouldIgnoreMetricWithInvalidValue() {
        ByteBuf buf = bufferFrom("metric.name test 1637856786");
        Metric metric = MetricReader.read(buf);
        assertNull(metric);
    }

    @Test
    public void shouldIgnoreMetricWithInvalidTimestamp() {
        ByteBuf buf = bufferFrom("metric.name 42 test");
        Metric metric = MetricReader.read(buf);
        assertNull(metric);
    }

    @Test
    public void shouldReadMetricWithNanValue() {
        ByteBuf buf = bufferFrom("metric.name NaN 1637856786");
        Metric metric = MetricReader.read(buf);
        assertNotNull(metric);

        assertArrayEquals("metric.name".getBytes(StandardCharsets.UTF_8), metric.name());
        assertFalse(metric.hasTags());
        assertTrue(Double.isNaN(metric.value()));
        assertEquals(1637856786L, metric.timestamp());
    }

    private ByteBuf bufferFrom(String s) {
        return Unpooled.wrappedBuffer(s.getBytes(StandardCharsets.UTF_8));
    }
}
