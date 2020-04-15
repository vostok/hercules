package ru.kontur.vostok.hercules.elastic.adapter.event;

import org.junit.Test;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.EventBuilder;
import ru.kontur.vostok.hercules.util.time.MockTimeSource;
import ru.kontur.vostok.hercules.util.time.TimeSource;
import ru.kontur.vostok.hercules.util.time.TimeUtil;
import ru.kontur.vostok.hercules.uuid.UuidGenerator;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Gregory Koshelev
 */
public class EventValidatorTest {
    @Test
    public void shouldFailOnExpiredEvent() {
        TimeSource time = new MockTimeSource();
        EventValidator validator = new EventValidator(time);

        Event event = EventBuilder.create(TimeUtil.millisToTicks(time.milliseconds()), UuidGenerator.getClientInstance().next()).build();
        assertTrue(validator.validate(event));

        time.sleep(TimeUnit.DAYS.toMillis(365));
        assertTrue(validator.validate(event));

        time.sleep(1);
        assertFalse(validator.validate(event));
    }

    @Test
    public void shouldFailOnEventFromDistantFuture() {
        TimeSource time = new MockTimeSource();
        EventValidator validator = new EventValidator(time);

        Event event = EventBuilder.create(
                TimeUtil.millisToTicks(time.milliseconds() + TimeUnit.DAYS.toMillis(30) + 1),
                UuidGenerator.getClientInstance().next()).
                build();
        assertFalse(validator.validate(event));

        time.sleep(1);
        assertTrue(validator.validate(event));
    }
}
