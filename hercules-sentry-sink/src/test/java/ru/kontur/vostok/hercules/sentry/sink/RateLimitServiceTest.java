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

    /**
     * maxRate = 1 per minute
     * 1th event -> true
     * 2th event -> false
     */
    @Test
    public void shouldBeTrueMaxRate1() {
        RateLimitService service = createLimiter(1, null, TimeUnit.MINUTES);
        assertTrue(service.updateAndCheck("someKeyProject"));
        assertFalse(service.updateAndCheck("someKeyProject"));
    }

    /**
     * maxRate = 3 per minute
     * 1 - 3th per sec event -> true
     * 4th event -> false
     */
    @Test
    public void shouldBeTrueMaxRate3() throws InterruptedException {
        RateLimitService service = createLimiter(3, null, TimeUnit.MINUTES);
        int n = 3;
        while (n-- > 0) {
            assertTrue(service.updateAndCheck("someKeyProject"));
            sleep(900);
        }
        assertFalse(service.updateAndCheck("someKeyProject"));
    }

    /**
     * maxRate = 1000 per minute
     * 1 - 1000th event at the same time -> true
     * 1001th event -> false
     */
    @Test
    public void shouldBeTrueMaxRate1000() {
        RateLimitService service = createLimiter(1000, null, TimeUnit.MINUTES);
        int n = 1000;
        while (n-- > 0) {
            assertTrue(service.updateAndCheck("someKeyProject"));
        }
        assertFalse(service.updateAndCheck("someKeyProject"));
    }

    /**
     * maxRate = 1 per sec
     * 1 - 4th event -> true
     * 5th event -> false
     * increase should be = 1
     */
    @Test
    public void shouldBeTrueMaxRate1PerSecWithIncrease1() throws InterruptedException {
        RateLimitService service = createLimiter(1, null, TimeUnit.SECONDS);
        int n = 3;
        while (n-- > 0) {
            assertTrue(service.updateAndCheck("someKeyProject"));
            sleep(1000);
        }
        assertTrue(service.updateAndCheck("someKeyProject"));
        assertFalse(service.updateAndCheck("someKeyProject"));
    }

    /**
     * Custom rule.
     * maxRate = 1 per sec
     * 1 - 4th event -> true
     * 5th event -> false
     * increase should be = 1
     */
    @Test
    public void shouldBeBeTrueWithCustomRule() throws InterruptedException {
        RateLimitService service = createLimiter(100500, "foo:500,someKeyProject:1,bar:5675535", TimeUnit.SECONDS);
        int n = 3;
        while (n-- > 0) {
            assertTrue(service.updateAndCheck("someKeyProject"));
            sleep(1000);
        }
        assertTrue(service.updateAndCheck("someKeyProject"));
        assertFalse(service.updateAndCheck("someKeyProject"));
    }

    @Test(expected = IllegalStateException.class)
    public void shouldBeExceptionWithIncorrectProperties() {
        createLimiter(-1, null, TimeUnit.SECONDS);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldBeExceptionWithIncorrectProperties2() {
        createLimiter(0, null, TimeUnit.SECONDS);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldBeExceptionWithIncorrectProperties3() {
        createLimiter(1, "some string", TimeUnit.SECONDS);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldBeExceptionWithIncorrectProperties4() {
        createLimiter(1, "somestring1,somestring2", TimeUnit.SECONDS);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldBeExceptionWithIncorrectProperties5() {
        createLimiter(1, "somestring:1 ,somestring:2", TimeUnit.SECONDS);
    }

    private RateLimitService createLimiter(int maxRate, String rules, TimeUnit timeUnit) {
        Properties properties = new Properties();
        properties.setProperty("limit", String.valueOf(maxRate));
        if (rules != null) {
            properties.setProperty("rules", rules);
        }
        return new RateLimitService(properties, timeUnit);
    }

}
