package ru.kontur.vostok.hercules.throttling.rate;

import org.junit.Test;

import java.util.Properties;

import static java.lang.Thread.sleep;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Artem Zhdanov
 * @author Gregory Koshelev
 */
public class RateLimiterTest {
    private static final String KEY = "key";

    /**
     * limit = 1 per sec
     * 1th event -> true
     * 2th event -> false
     */
    @Test
    public void shouldBeTrueWithLimit1PerSec() {
        RateLimiter service = createLimiter(1, 1_000L);
        assertTrue(service.updateAndCheck(KEY));
        assertFalse(service.updateAndCheck(KEY));
    }

    /**
     * limit = 1 per sec
     * 1 - 4th events with lower rate -> true
     * 5th event raised immidiately with empty quota -> false
     */
    @Test
    public void shouldBeTrueWithLimit1PerSecAndCorrectIncrease() throws InterruptedException {
        RateLimiter service = createLimiter(1, 1_000L);
        assertTrue(service.updateAndCheck(KEY));
        sleep(1_000);
        assertTrue(service.updateAndCheck(KEY));
        sleep(1_000);
        assertTrue(service.updateAndCheck(KEY));
        sleep(1_000);
        assertTrue(service.updateAndCheck(KEY));
        assertFalse(service.updateAndCheck(KEY));
    }

    @Test
    public void shouldBeDropLargeLoad() {
        RateLimiter service = createLimiter(10_000, 3_000L);
        assertEquals(50f, sendAndCalculateDroppedPercent(service, 20_000), 2f);
        assertEquals(100f, sendAndCalculateDroppedPercent(service, 20_000), 1f);
        assertEquals(100f, sendAndCalculateDroppedPercent(service, 20_000), 1f);
    }

    @Test
    public void shouldBeRecoverWithAverageSize() throws InterruptedException {
        RateLimiter service = createLimiter(1_000, 1_000L);
        assertEquals(0f, sendAndCalculateDroppedPercent(service, 350), 1f);
        sleep(1_000);
        assertEquals(95f, sendAndCalculateDroppedPercent(service, 20_000), 2f);
        sleep(1_000);
        assertEquals(0f, sendAndCalculateDroppedPercent(service, 350), 1f);
        sleep(1_000);
        assertEquals(10f, sendAndCalculateDroppedPercent(service, 1_100), 4f);
        sleep(1_000);
        assertEquals(0f, sendAndCalculateDroppedPercent(service, 350), 1f);
    }

    private float sendAndCalculateDroppedPercent(RateLimiter rateLimiter, int requestCount) {
        int droppedCount = 0;
        for (int i = 0; i < requestCount; i++) {
            if (!rateLimiter.updateAndCheck(KEY)) {
                droppedCount++;
            }
        }
        return droppedCount * 100f / requestCount;
    }

    @Test(expected = IllegalStateException.class)
    public void shouldBeExceptionWithNegativeLimit() {
        createLimiter(-1, 1_000L);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldBeExceptionWithZeroLimit() {
        createLimiter(0, 1_000L);
    }

    private RateLimiter createLimiter(long limit, long timeWindowMs) {
        Properties properties = new Properties();
        properties.setProperty(RateLimiter.Props.LIMIT.name(), String.valueOf(limit));
        properties.setProperty(RateLimiter.Props.TIME_WINDOW_MS.name(), String.valueOf(timeWindowMs));
        return new RateLimiter(properties);
    }

}
