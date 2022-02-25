package ru.kontur.vostok.hercules.json.format.combiner;

import org.junit.Test;
import ru.kontur.vostok.hercules.protocol.Variant;

import static org.junit.Assert.assertEquals;

/**
 * @author Gregory Koshelev
 */
public class CombinerTest {
    @Test
    public void testIsoDateTimeCombiner() {
        Variant timestamp = Variant.ofLong(15954978501499766L);
        Variant offset = Variant.ofLong(108000000000L);

        Combiner combiner = new IsoDateTimeCombiner();
        assertEquals("2020-07-23T12:50:50.149976600+03:00", combiner.combine(timestamp, offset));
        assertEquals("2020-07-23T09:50:50.149976600Z", combiner.combine(timestamp));
        assertEquals("2020-07-23T09:50:50.149976600Z", combiner.combine(timestamp, null));
    }

    @Test
    public void testLatencyMsCombiner() {
        Variant beginTimestamp = Variant.ofLong(15954978501499766L);
        Variant endTimestamp = Variant.ofLong(15954978501533487L);

        Combiner combiner = new LatencyMsCombiner();
        assertEquals(3L, combiner.combine(beginTimestamp, endTimestamp));
    }

    @Test
    public void testLatencyStringCombiner() {
        Variant beginTimestamp = Variant.ofLong(15954978501499766L);
        Variant endTimestamp = Variant.ofLong(15954978501533487L);

        Combiner combiner = new LatencyStringCombiner();
        assertEquals("3.372 milliseconds", combiner.combine(beginTimestamp, endTimestamp));
    }
}
