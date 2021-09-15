import org.junit.Test;
import ru.kontur.vostok.hercules.health.MetricsUtil;

import static org.junit.Assert.assertEquals;

/**
 * @author Gregory Koshelev
 */
public class MetricsUtilTest {
    @Test
    public void shouldBuildSanitizedMetricName() {
        assertEquals("impossibly_cool_metric_name", MetricsUtil.toMetricName("impossibly cool", "metric.name"));
    }

    @Test
    public void shouldSanitizeMetricName() {
        assertEquals("sanitized_metric_name", MetricsUtil.sanitizeMetricName("sanitized_metric_name"));
        assertEquals("1234567890_should_be_ok", MetricsUtil.sanitizeMetricName("1234567890_should_be_ok"));
        assertEquals("whitespace_metric_name", MetricsUtil.sanitizeMetricName("whitespace metric name"));
        assertEquals("____is_illegal__isn_t_", MetricsUtil.sanitizeMetricName("[.] is illegal, isn't?"));
        assertEquals("http:__is_url_but_20sanitized", MetricsUtil.sanitizeMetricName("http://is.url/but%20sanitized"));
    }
}
