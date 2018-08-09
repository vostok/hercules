import com.codahale.metrics.Clock;
import com.codahale.metrics.Counter;
import com.codahale.metrics.ExponentiallyDecayingReservoir;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricAttribute;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Snapshot;
import com.codahale.metrics.Timer;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import ru.kontur.vostok.hercules.micrometer.registry.HerculesMetricFormatter;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.HerculesProtocolAssert;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.protocol.util.EventBuilder;
import ru.kontur.vostok.hercules.uuid.UuidGenerator;

/**
 * Skipping metric attribute tested. See {@code shouldConvertTimerMetricCorrect} and {@code shouldConvertMeterMetricCorrect}
 *
 * @author Daniil Zhenikhov
 */
public class HerculesMetricFormatterTests {
    private static final String GAUGE_METRIC_NAME = "gauge_test";
    private static final String COUNTER_METRIC_NAME = "counter_test";
    private static final String HISTOGRAM_METRIC_NAME = "histogram_test";
    private static final String METER_METRIC_NAME = "meter_test";
    private static final String TIMER_METRIC_NAME = "timer_test";
    private static final String RATE_UNIT = calculateRateUnit();
    private static final String DURATION_UNIT = TimeUnit.MILLISECONDS.toString().toLowerCase(Locale.US);
    private static final Long COUNTER_SUM_VALUE = 3L;
    private static final long TIMESTAMP = 0;
    private static final HerculesMetricFormatter FORMATTER = new HerculesMetricFormatter(
            RATE_UNIT,
            DURATION_UNIT,
            new HashSet<>(Collections.singletonList(MetricAttribute.MEAN_RATE)));

    private final UuidGenerator GENERATOR = UuidGenerator.getClientInstance();
    private final Clock CLOCK = Mockito.mock(Clock.class);
    private Gauge gaugeTest;
    private Counter counterTest;
    private Histogram histogramTest;
    private Meter meterTest;
    private Timer timerTest;
    private long TICK = TimeUnit.SECONDS.toNanos(0);

    private static String calculateRateUnit() {
        final String s = TimeUnit.MILLISECONDS.toString().toLowerCase(Locale.US);
        return s.substring(0, s.length() - 1);
    }

    @Before
    public void setUp() {
        Mockito.when(CLOCK.getTick()).then(invocation ->
                TICK = TICK + TimeUnit.MILLISECONDS.toNanos(400));

        MetricRegistry registry = new MetricRegistry();
        gaugeTest = () -> GAUGE_METRIC_NAME;
        counterTest = registry.counter(COUNTER_METRIC_NAME);
        histogramTest = registry.histogram(HISTOGRAM_METRIC_NAME);
        meterTest = new Meter(CLOCK);
        timerTest = new Timer(new ExponentiallyDecayingReservoir(), CLOCK);
    }

    @Test
    public void shouldConvertGaugeMetricCorrect() {
        Event expected = eventGauge();
        Event actual = FORMATTER.formatGauge(GAUGE_METRIC_NAME, gaugeTest, TIMESTAMP);

        HerculesProtocolAssert.assertEquals(expected, actual, false, false);
    }

    @Test
    public void shouldConvertCounterMetricCorrect() {
        counterTest.inc(COUNTER_SUM_VALUE);
        Event expected = eventCounter();
        Event actual = FORMATTER.formatCounter(COUNTER_METRIC_NAME, counterTest, TIMESTAMP);

        HerculesProtocolAssert.assertEquals(expected, actual, false, false);
    }

    @Test
    public void shouldConvertHistogramMetricCorrect() {
        histogramTest.update(200);

        Event expected = eventHistogram(histogramTest);
        Event actual = FORMATTER.formatHistogram(HISTOGRAM_METRIC_NAME, histogramTest, TIMESTAMP);

        HerculesProtocolAssert.assertEquals(expected, actual, false, false);
    }

    /**
     * Skipped metric attribute "MEAN_RATE"
     */
    @Test
    public void shouldConvertMeterMetricCorrect() throws InterruptedException {
        for (int i = 0; i < 12; i++) {
            meterTest.mark(100);
        }

        Event expected = eventMeter(meterTest);
        Event actual = FORMATTER.formatMeter(METER_METRIC_NAME, meterTest, TIMESTAMP);

        HerculesProtocolAssert.assertEquals(expected, actual, false, false);
    }

    @Test
    public void shouldConvertTimerMetricCorrect() throws Exception {
        timerTest.time(() -> doSomething(1000));

        Event expected = eventTimer(timerTest);
        Event actual = FORMATTER.formatTimer(TIMER_METRIC_NAME, timerTest, TIMESTAMP);

        HerculesProtocolAssert.assertEquals(expected, actual, false, false);
    }

    private int doSomething(int limit) {
        int sum = 0;

        for (int i = 0; i < limit; i++) {
            sum += i;
        }

        return sum;
    }

    private Event eventTimer(Timer timer) {
        Snapshot snapshot = timer.getSnapshot();

        Map<String, Variant> map = new HashMap<>();
        map.put("name", Variant.ofString(TIMER_METRIC_NAME));
        map.put(MetricAttribute.COUNT.getCode(), Variant.ofLong(timer.getCount()));

        /* SKIPPED
        map.put(MetricAttribute.MEAN_RATE.getCode(), Variant.ofDouble(meter.getMeanRate()));
        */

        map.put(MetricAttribute.M1_RATE.getCode(), Variant.ofDouble(timer.getOneMinuteRate()));
        map.put(MetricAttribute.M5_RATE.getCode(), Variant.ofDouble(timer.getFiveMinuteRate()));
        map.put(MetricAttribute.M15_RATE.getCode(), Variant.ofDouble(timer.getFifteenMinuteRate()));
        map.put(MetricAttribute.MAX.getCode(), Variant.ofLong(snapshot.getMax()));
        map.put(MetricAttribute.MEAN.getCode(), Variant.ofDouble(snapshot.getMean()));
        map.put(MetricAttribute.MIN.getCode(), Variant.ofLong(snapshot.getMin()));
        map.put(MetricAttribute.STDDEV.getCode(), Variant.ofDouble(snapshot.getStdDev()));
        map.put(MetricAttribute.P50.getCode(), Variant.ofDouble(snapshot.getMedian()));
        map.put(MetricAttribute.P75.getCode(), Variant.ofDouble(snapshot.get75thPercentile()));
        map.put(MetricAttribute.P95.getCode(), Variant.ofDouble(snapshot.get95thPercentile()));
        map.put(MetricAttribute.P98.getCode(), Variant.ofDouble(snapshot.get98thPercentile()));
        map.put(MetricAttribute.P99.getCode(), Variant.ofDouble(snapshot.get99thPercentile()));
        map.put(MetricAttribute.P999.getCode(), Variant.ofDouble(snapshot.get999thPercentile()));
        map.put("rate_unit", Variant.ofString(RATE_UNIT));
        map.put("duration_unit", Variant.ofString(DURATION_UNIT));

        return baseEvent(map);
    }

    private Event eventMeter(Meter meter) {
        Map<String, Variant> map = new HashMap<>();
        map.put("name", Variant.ofString(METER_METRIC_NAME));
        map.put(MetricAttribute.COUNT.getCode(), Variant.ofLong(meter.getCount()));

        /* SKIPPED
        map.put(MetricAttribute.MEAN_RATE.getCode(), Variant.ofDouble(meter.getMeanRate()));
        */

        map.put(MetricAttribute.M1_RATE.getCode(), Variant.ofDouble(meter.getOneMinuteRate()));
        map.put(MetricAttribute.M5_RATE.getCode(), Variant.ofDouble(meter.getFiveMinuteRate()));
        map.put(MetricAttribute.M15_RATE.getCode(), Variant.ofDouble(meter.getFifteenMinuteRate()));
        map.put("rate_unit", Variant.ofString(RATE_UNIT));

        return baseEvent(map);
    }

    private Event eventHistogram(Histogram histogram) {
        Snapshot snapshot = histogram.getSnapshot();

        Map<String, Variant> map = new HashMap<>();
        map.put("name", Variant.ofString(HISTOGRAM_METRIC_NAME));
        map.put(MetricAttribute.COUNT.getCode(), Variant.ofLong(histogram.getCount()));
        map.put(MetricAttribute.MAX.getCode(), Variant.ofLong(snapshot.getMax()));
        map.put(MetricAttribute.MEAN.getCode(), Variant.ofDouble(snapshot.getMean()));
        map.put(MetricAttribute.MIN.getCode(), Variant.ofLong(snapshot.getMin()));
        map.put(MetricAttribute.STDDEV.getCode(), Variant.ofDouble(snapshot.getStdDev()));
        map.put(MetricAttribute.P50.getCode(), Variant.ofDouble(snapshot.getMedian()));
        map.put(MetricAttribute.P75.getCode(), Variant.ofDouble(snapshot.get75thPercentile()));
        map.put(MetricAttribute.P95.getCode(), Variant.ofDouble(snapshot.get95thPercentile()));
        map.put(MetricAttribute.P98.getCode(), Variant.ofDouble(snapshot.get98thPercentile()));
        map.put(MetricAttribute.P99.getCode(), Variant.ofDouble(snapshot.get99thPercentile()));
        map.put(MetricAttribute.P999.getCode(), Variant.ofDouble(snapshot.get999thPercentile()));

        return baseEvent(map);
    }

    private Event eventGauge() {
        Map<String, Variant> map = new HashMap<>();
        map.put("name", Variant.ofString(GAUGE_METRIC_NAME));
        map.put("value", Variant.ofString(GAUGE_METRIC_NAME));

        return baseEvent(map);
    }

    private Event eventCounter() {
        Map<String, Variant> map = new HashMap<>();
        map.put("name", Variant.ofString(COUNTER_METRIC_NAME));
        map.put(MetricAttribute.COUNT.getCode(), Variant.ofLong(COUNTER_SUM_VALUE));

        return baseEvent(map);
    }

    private Event baseEvent(Map<String, Variant> map) {
        EventBuilder eventBuilder = new EventBuilder();

        eventBuilder.setVersion(1);
        eventBuilder.setEventId(GENERATOR.next());
        eventBuilder.setTag("timestamp", Variant.ofLong(TIMESTAMP));

        map.forEach(eventBuilder::setTag);

        return eventBuilder.build();
    }
}