package ru.kontur.vostok.hercules.graphite.adapter.purgatory;

import org.junit.Assert;
import org.junit.Test;
import ru.kontur.vostok.hercules.graphite.adapter.metric.Metric;

import java.nio.charset.StandardCharsets;

/**
 * @author Petr Demenev
 */
public class MetricValidatorTest {
    private static final MetricValidator metricValidator = new MetricValidator();
    private static final long TIMESTAMP = 16347347520000000L;

    @Test
    public void shouldPassValidMetric() {
        String name = "one.Two.THREE-4.five_6.seven:8";
        Metric metric = new Metric(name.getBytes(StandardCharsets.UTF_8), null, 0.1D,TIMESTAMP);
        Assert.assertTrue(metricValidator.validate(metric));
    }

    @Test
    public void shouldNotPassNaN() {
        Metric metric = new Metric("someName".getBytes(StandardCharsets.UTF_8), null, Double.NaN,TIMESTAMP);

        Assert.assertFalse(metricValidator.validate(metric));
    }

    @Test
    public void shouldNotPassEmptyMetricName() {
        Metric metric = new Metric(new byte[0], null, 0.1D,TIMESTAMP);
        Assert.assertFalse(metricValidator.validate(metric));
    }

    @Test
    public void shouldNotPassMetricNameWithInvalidCharacter() {
        Metric metric = new Metric("x=y".getBytes(StandardCharsets.UTF_8), null, 0.1D,TIMESTAMP);
        Assert.assertFalse(metricValidator.validate(metric));
    }
}
