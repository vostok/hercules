import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricAttribute;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Snapshot;
import com.codahale.metrics.Timer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.BeforeClass;
import org.junit.Test;
import ru.kontur.vostok.hercules.micrometer.registry.HerculesMetricFormatter;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.HerculesProtocolAssert;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.protocol.WriteReadPipe;
import ru.kontur.vostok.hercules.protocol.decoder.EventReader;
import ru.kontur.vostok.hercules.protocol.encoder.EventBuilder;
import ru.kontur.vostok.hercules.protocol.encoder.EventWriter;
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
    private static final String RATE_UNIT = calculateRateUnit(TimeUnit.MILLISECONDS);
    private static final String DURATION_UNIT = TimeUnit.MILLISECONDS.toString().toLowerCase(Locale.US);
    private static final Long COUNTER_SUM_VALUE = 3L;

    private static final Set<String> readableTags = Stream.concat(
            Stream.of("name", "value", "rate_unit", "duration_unit"),
            Arrays.stream(MetricAttribute.values()).map(MetricAttribute::getCode)
    ).collect(Collectors.toSet());

    private static final HerculesMetricFormatter FORMATTER = new HerculesMetricFormatter(
            RATE_UNIT,
            DURATION_UNIT,
            new HashSet<>(Arrays.asList(MetricAttribute.MEAN_RATE)));

    private static final UuidGenerator GENERATOR = UuidGenerator.getClientInstance();

    private static final WriteReadPipe<Event> PIPE = WriteReadPipe.init(
            new EventWriter(),
            EventReader.readTags(readableTags));


    private static Gauge gaugeTest;
    private static Counter counterTest;
    private static Histogram histogramTest;
    private static Meter meterTest;
    private static Timer timerTest;

    @BeforeClass
    public static void setUp() {
        MetricRegistry registry = new MetricRegistry();
        gaugeTest = () -> GAUGE_METRIC_NAME;
        counterTest = registry.counter(COUNTER_METRIC_NAME);
        histogramTest = registry.histogram(HISTOGRAM_METRIC_NAME);
        meterTest = registry.meter(METER_METRIC_NAME);
        timerTest = registry.timer(TIMER_METRIC_NAME);
    }

    private static String calculateRateUnit(TimeUnit unit) {
        final String s = unit.toString().toLowerCase(Locale.US);
        return s.substring(0, s.length() - 1);
    }

    @Test
    public void shouldConvertGaugeMetricCorrect() {
        Event expected = PIPE.process(eventGauge()).getProcessed();
        Event actual = PIPE
                .process(FORMATTER.formatGauge(GAUGE_METRIC_NAME, gaugeTest, 0))
                .getProcessed();

        HerculesProtocolAssert.assertEquals(expected, actual, false, false);
    }

    @Test
    public void shouldConvertCounterMetricCorrect() {
        counterTest.inc(COUNTER_SUM_VALUE);
        Event expected = PIPE.process(eventCounter()).getProcessed();
        Event actual = PIPE
                .process(FORMATTER.formatCounter(COUNTER_METRIC_NAME, counterTest, 0))
                .getProcessed();

        HerculesProtocolAssert.assertEquals(expected, actual, false, false);
    }

    @Test
    public void shouldConvertHistogramMetricCorrect() {
        histogramTest.update(200);
        Event expected = PIPE.process(eventHistogram(histogramTest)).getProcessed();
        Event actual = PIPE
                .process(FORMATTER.formatHistogram(HISTOGRAM_METRIC_NAME, histogramTest, 0))
                .getProcessed();

        HerculesProtocolAssert.assertEquals(expected, actual, false, false);
    }

    /**
     * Skipped metric attribute "MEAN_RATE"
     */
    @Test
    public void shouldConvertMeterMetricCorrect() throws InterruptedException {
        meterTest.mark(100);
        Thread.sleep(10000);

        Event expected = PIPE.process(eventMeter(meterTest)).getProcessed();
        Event actual = PIPE
                .process(FORMATTER.formatMeter(METER_METRIC_NAME, meterTest, 0))
                .getProcessed();

        HerculesProtocolAssert.assertEquals(expected, actual, false, false);
    }

    @Test
    public void shouldConvertTimerMetricCorrect() {
        timerTest.time(() -> {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        Event expected = PIPE.process(eventTimer(timerTest)).getProcessed();
        Event actual = PIPE
                .process(FORMATTER.formatTimer(TIMER_METRIC_NAME, timerTest, 0))
                .getProcessed();

        HerculesProtocolAssert.assertEquals(expected, actual, false, false);
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

        map.forEach(eventBuilder::setTag);

        return eventBuilder.build();
    }
}
