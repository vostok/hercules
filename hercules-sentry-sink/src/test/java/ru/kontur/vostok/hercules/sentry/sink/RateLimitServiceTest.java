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
     * maxRate = 1 per minute
     * 1th event -> true
     * 2th event -> false
     */
    @Test
    public void shouldBeTrueMaxRate1() {
        RateLimitService service = createLimiter(1);
        assertTrue(service.updateAndCheck(KEY));
        assertFalse(service.updateAndCheck(KEY));
    }

    /**
     * maxRate = 1 per sec
     * 1th event -> true
     * 2th event -> false
     */
    @Test
    public void shouldBeTrueMaxRate1Sec() {
        RateLimitService service = createLimiter(1, 1, TimeUnit.SECONDS);
        assertTrue(service.updateAndCheck(KEY));
        assertFalse(service.updateAndCheck(KEY));
    }

    /**
     * maxRate = 3 per minute
     * 1 - 3th per sec event -> true
     * 4th event -> false
     */
    @Test
    public void shouldBeTrueMaxRate3() throws InterruptedException {
        RateLimitService service = createLimiter(3);
        int n = 3;
        while (n-- > 0) {
            assertTrue(service.updateAndCheck(KEY));
            sleep(900);
        }
        assertFalse(service.updateAndCheck(KEY));
    }

    /**
     * maxRate = 1000 per minute, with some threads
     */
    @Test
    public void shouldBeTrueWithSomeThreads() throws InterruptedException {
        RateLimitService service = createLimiter(1000);
        Runnable runnable = () -> {
            for (int n = 0; n < 200; n++) {
                service.updateAndCheck(KEY);
            }
        };
        new Thread(runnable).start();
        new Thread(runnable).start();
        new Thread(runnable).start();
        new Thread(runnable).start();
        new Thread(runnable).start();
        sleep(1000);
        float val = service.getValue(KEY);
        assertTrue(val > -1 && val < 10);
        System.out.println("Value: " + service.getValue(KEY));
    }

    /**
     * maxRate = 1 per sec
     * 1 - 4th event -> true
     * 5th event -> false
     * increase should be = 1
     * with some threads
     */
    @Test
    public void shouldBeTrueMaxRate1PerSecWithIncrease1() throws InterruptedException {
        RateLimitService service = createLimiter(1, 1L, TimeUnit.SECONDS);
        Runnable runnable = () -> assertTrue(service.updateAndCheck(KEY));
        int n = 3;
        while (n-- > 0) {
            new Thread(runnable).start();
            sleep(1000);
        }
        assertTrue(service.updateAndCheck(KEY));
        assertFalse(service.updateAndCheck(KEY));
    }

    @Test(expected = IllegalStateException.class)
    public void shouldBeExceptionWithIncorrectProperties() {
        createLimiter(-1);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldBeExceptionWithIncorrectProperties2() {
        createLimiter(0, 0, TimeUnit.SECONDS);
    }

    private RateLimitService createLimiter(int maxRate) {
        return createLimiter(maxRate, 0, null);
    }

    private RateLimitService createLimiter(int maxRate, long timeWindow, TimeUnit timeUnit) {
        Properties properties = new Properties();
        properties.setProperty("limit", String.valueOf(maxRate));
        if (timeWindow != 0) {
            properties.setProperty("timeWindow", String.valueOf(timeWindow));
        }
        if (timeUnit != null) {
            properties.setProperty("timeUnit", timeUnit.name());
        }
        return new RateLimitService(properties);
    }

}
