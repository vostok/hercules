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
        if (event.getId().version() != 1) { // ID of Event should be UUID version 1
            return false;
        }

        if (TimeUtil.gregorianTicksToUnixTime(event.getId().timestamp()) < 0) { // Event timestamp should be positive
            return false;
        }

        return true;
    }
}
