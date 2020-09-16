package ru.kontur.vostok.hercules.elastic.adapter.format;

import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.util.time.TimeSource;
import ru.kontur.vostok.hercules.util.time.TimeUtil;

import java.util.concurrent.TimeUnit;

/**
 * Event validator.
 * <p>
 * Validates event which is parsed from request.
 *
 * @author Gregory Koshelev
 */
public class EventValidator {
    private static final long YEAR_TICKS = TimeUtil.secondsToTicks(TimeUnit.DAYS.toSeconds(365));
    private static final long MONTH_TICKS = TimeUtil.secondsToTicks(TimeUnit.DAYS.toSeconds(30));

    private final TimeSource time;

    public EventValidator() {
        this(TimeSource.SYSTEM);
    }

    EventValidator(TimeSource time) {
        this.time = time;
    }

    /**
     * Check if event timestamp is inside the processable range {@code [now - year; now + month]}.
     *
     * @param event the event
     * @return {@code true} if event is valid, otherwise return {@code false}
     */
    public boolean validate(Event event) {
        long now = TimeUtil.millisToTicks(time.milliseconds());
        long timestamp = event.getTimestamp();
        return now - YEAR_TICKS <= timestamp && timestamp <= now + MONTH_TICKS;
    }
}
