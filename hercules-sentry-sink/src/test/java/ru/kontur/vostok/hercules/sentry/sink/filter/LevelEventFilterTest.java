package ru.kontur.vostok.hercules.sentry.sink.filter;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.EventBuilder;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.util.time.TimeUtil;

import java.util.Properties;

/**
 * @author Petr Demenev
 */
public class LevelEventFilterTest {

    private static LevelEventFilter filter;

    @BeforeClass
    public static void init() {
        Properties properties = new Properties();
        properties.setProperty("level", "WARNING"); //This value does not equal default value "ERROR"
        filter = new LevelEventFilter(properties);
    }

    @Test
    public void shouldReturnTrueIfLevelEqualsWithRequired() {
        Event event = getEventBuilder().
                tag("level", Variant.ofString("warning")).
                build();

        Assert.assertTrue(filter.test(event));
    }

    @Test
    public void shouldReturnTrueIfLevelIsHigherThenRequired() {
        Event event = getEventBuilder().
                tag("level", Variant.ofString("fatal")).
                build();

        Assert.assertTrue(filter.test(event));
    }

    @Test
    public void shouldReturnFalseIfLevelIsLowerThenRequired() {
        Event event = getEventBuilder().
                tag("level", Variant.ofString("info")).
                build();

        Assert.assertFalse(filter.test(event));
    }

    @Test
    public void shouldReturnFalseIfLevelTagIsAbsent() {
        Event event = getEventBuilder().build();

        Assert.assertFalse(filter.test(event));
    }

    private EventBuilder getEventBuilder() {
        return EventBuilder.create(
                TimeUtil.millisToTicks(System.currentTimeMillis()),
                "00000000-0000-0000-0000-000000000000");
    }
}
