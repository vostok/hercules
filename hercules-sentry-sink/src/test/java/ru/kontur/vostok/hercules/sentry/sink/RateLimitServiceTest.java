package ru.kontur.vostok.hercules.sentry.sink;

import org.junit.Test;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static java.lang.Thread.sleep;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Artem Zhdanov
 */
public class RateLimitServiceTest {

    private static final String KEY = "SOME_PROJECT_KEY";

    /**
     * limit = 1 per minute
     * 1th event -> true
     * 2th event -> false
     */
    @Test
    public void shouldBeTrueWithLimit1PerMin() {
        RateLimitService service = createLimiter(1);
        assertTrue(service.updateAndCheck(KEY));
        assertFalse(service.updateAndCheck(KEY));
    }

    /**
     * limit = 1000 per minute
     * 1 - 1000th event -> true
     * 1001th event -> false
     */
    @Test
    public void shouldBeTrueWithLimit1000PerMin() {
        RateLimitService service = createLimiter(1000);
        for (int i = 0; i < 1000; i++) {
            assertTrue(service.updateAndCheck(KEY));
        }
        assertFalse(service.updateAndCheck(KEY));
    }

    /**
     * limit = 1 per sec
     * 1th event -> true
     * 2th event -> false
     */
    @Test
    public void shouldBeTrueWithLimit1PerSec() {
        RateLimitService service = createLimiter(1, 1, TimeUnit.SECONDS);
        assertTrue(service.updateAndCheck(KEY));
        assertFalse(service.updateAndCheck(KEY));
    }

    /**
     * limit = 1 per sec
     * 1 - 4th event -> true
     * 5th event -> false
     */
    @Test
    public void shouldBeTrueWithLimit1PerSecAndCorrectIncrease() throws InterruptedException {
        RateLimitService service = createLimiter(1, 1, TimeUnit.SECONDS);
        for (int i = 0; i < 3; i++) {
            assertTrue(service.updateAndCheck(KEY));
            sleep(1000);
        }
        assertTrue(service.updateAndCheck(KEY));
        assertFalse(service.updateAndCheck(KEY));
    }

    @Test(expected = IllegalStateException.class)
    public void shouldBeExceptionWithNegativeLimit() {
        createLimiter(-1);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldBeExceptionWithZeroLimit() {
        createLimiter(0, 0, TimeUnit.SECONDS);
    }

    private RateLimitService createLimiter(long limit) {
        return createLimiter(limit, 0, null);
    }

    private RateLimitService createLimiter(long limit, long timeWindow, TimeUnit timeUnit) {
        Properties properties = new Properties();
        properties.setProperty("limit", String.valueOf(limit));
        if (timeWindow != 0) {
            properties.setProperty("timeWindow", String.valueOf(timeWindow));
        }
        if (timeUnit != null) {
            properties.setProperty("timeUnit", timeUnit.name());
        }
        return new RateLimitService(properties);
    }

}
