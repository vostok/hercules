package ru.kontur.vostok.hercules.gate.validation;

import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.util.time.TimeUtil;

/**
 * @author Gregory Koshelev
 */
public class EventValidator {
    public boolean validate(Event event) {
        if (event.getVersion() != 1) { // Gate supports Event of version 1 only
            return false;
        }

        if (TimeUtil.gregorianTicksToUnixTime(event.getTimestamp()) < 0) { // Event timestamp should be positive
            return false;
        }

        return true;
    }
}
