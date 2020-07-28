package ru.kontur.vostok.hercules.util.time;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Gregory Koshelev
 */
public class TimeUtilTest {
    @Test
    public void testPrettyPrint() {
        long ticks = 1L;
        assertEquals("0.1 microseconds", TimeUtil.ticksToPrettyString(ticks, false));

        ticks = 1234L;
        assertEquals("123.4us", TimeUtil.ticksToPrettyString(ticks, true));

        ticks = 12345;//Banker's rounding will be used
        assertEquals("1.234ms", TimeUtil.ticksToPrettyString(ticks, true));

        ticks = 12345678L;
        assertEquals("1.235s", TimeUtil.ticksToPrettyString(ticks, true));

        ticks = 123456789L;
        assertEquals("12.346s", TimeUtil.ticksToPrettyString(ticks, true));

        ticks = 690000000L;
        assertEquals("1.15m", TimeUtil.ticksToPrettyString(ticks, true));

        ticks = 6900000000L;
        assertEquals("11.5m", TimeUtil.ticksToPrettyString(ticks, true));

        ticks = 43200000000L;
        assertEquals("1.2h", TimeUtil.ticksToPrettyString(ticks, true));

        ticks = 864000000000L;
        assertEquals("1d", TimeUtil.ticksToPrettyString(ticks, true));
    }
}
