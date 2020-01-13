package ru.kontur.vostok.hercules.gate.validation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.protocol.Event;

/**
 * @author Gregory Koshelev
 */
public class EventValidator {
    private static final Logger LOGGER = LoggerFactory.getLogger(EventValidator.class);

    public boolean validate(Event event) {
        if (event.getVersion() != 1) { // Gate supports Event of version 1 only
            LOGGER.warn("Event version != 1");
            return false;
        }

        if (event.getTimestamp() < 0) { // Event timestamp should be non-negative
            LOGGER.warn("Event timestamp < 0");
            return false;
        }

        return true;
    }
}
