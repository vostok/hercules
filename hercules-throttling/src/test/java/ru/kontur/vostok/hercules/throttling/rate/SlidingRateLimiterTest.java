package ru.kontur.vostok.hercules.throttling.rate;

import org.junit.Test;
import ru.kontur.vostok.hercules.util.time.MockTimeSource;
import ru.kontur.vostok.hercules.util.time.TimeSource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Gregory Koshelev
 */
public class SlidingRateLimiterTest {
    private static TimeSource TIME = new MockTimeSource();

    /**
     * 1 RPS limit should works as described:
     * <pre>
     * |   time   | event | result |
     * |     0 ms |   1   |     OK |
     * |   500 ms |   2   |      X |
     * |   750 ms |   3   |      X |
     * | 1 000 ms |   4   |     OK |
     * | 1 500 ms |   5   |      X |
     * | 2 000 ms |   6   |     OK |
     * </pre>
     */
    @Test
    public void shouldLimitRpsCorrectly() {
        SlidingRateLimiter oneRps = new SlidingRateLimiter(1, 1_000, TIME.milliseconds());

        assertTrue(oneRps.updateAndCheck(TIME.milliseconds()));

        TIME.sleep(500);
        assertFalse(oneRps.updateAndCheck(TIME.milliseconds()));

        TIME.sleep(250);
        assertFalse(oneRps.updateAndCheck(TIME.milliseconds()));

        TIME.sleep(250);
        assertTrue(oneRps.updateAndCheck(TIME.milliseconds()));

        TIME.sleep(500);
        assertFalse(oneRps.updateAndCheck(TIME.milliseconds()));

        TIME.sleep(500);
        assertTrue(oneRps.updateAndCheck(TIME.milliseconds()));
    }

    /**
     * 1 RPS limit.
     * <p>
     * Accept 4 events with low RP.
     * But immediately deny 5th event as rate limit is exceeded.
     */
    @Test
    public void shouldLimitRpsIfRateIsIncreased() {
        SlidingRateLimiter service = new SlidingRateLimiter(1, 1_000L, TIME.milliseconds());

        assertTrue(service.updateAndCheck(TIME.milliseconds()));

        TIME.sleep(2_000);
        assertTrue(service.updateAndCheck(TIME.milliseconds()));

        TIME.sleep(2_000);
        assertTrue(service.updateAndCheck(TIME.milliseconds()));

        TIME.sleep(2_000);
        assertTrue(service.updateAndCheck(TIME.milliseconds()));
        assertFalse(service.updateAndCheck(TIME.milliseconds()));
    }

    @Test
    public void shouldDropExtraLoad() {
        SlidingRateLimiter service = new SlidingRateLimiter(10_000, 3_000L, TIME.milliseconds());

        assertEquals(50f, sendAndCalculateDroppedPercent(service, 20_000), 1f);
        assertEquals(100f, sendAndCalculateDroppedPercent(service, 20_000), 1f);
        assertEquals(100f, sendAndCalculateDroppedPercent(service, 20_000), 1f);
    }

    @Test
    public void shouldRecoverLimits() {
        SlidingRateLimiter service = new SlidingRateLimiter(1_000, 1_000L, TIME.milliseconds());

        assertEquals(0f, sendAndCalculateDroppedPercent(service, 350), 1f);

        TIME.sleep(1_000);
        assertEquals(95f, sendAndCalculateDroppedPercent(service, 20_000), 1f);

        TIME.sleep(1_000);
        assertEquals(0f, sendAndCalculateDroppedPercent(service, 350), 1f);

        TIME.sleep(1_000);
        assertEquals(10f, sendAndCalculateDroppedPercent(service, 1_100), 1f);

        TIME.sleep(1_000);
        assertEquals(0f, sendAndCalculateDroppedPercent(service, 350), 1f);
    }

    private float sendAndCalculateDroppedPercent(SlidingRateLimiter rateLimiter, int requestCount) {
        int droppedCount = 0;
        for (int i = 0; i < requestCount; i++) {
            if (!rateLimiter.updateAndCheck(TIME.milliseconds())) {
                droppedCount++;
            }
        }
        return droppedCount * 100f / requestCount;
    }
}
