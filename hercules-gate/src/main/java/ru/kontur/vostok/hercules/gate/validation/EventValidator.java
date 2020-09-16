package ru.kontur.vostok.hercules.gate.validation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.util.parameter.Parameter;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;
import ru.kontur.vostok.hercules.util.validation.IntegerValidators;

import java.util.Properties;

/**
 * @author Gregory Koshelev
 */
public class EventValidator {
    private static final Logger LOGGER = LoggerFactory.getLogger(EventValidator.class);

    private final int maxEventSize;

    public EventValidator(Properties properties) {
        this.maxEventSize = PropertiesUtil.get(Props.MAX_EVENT_SIZE, properties).get();
    }

    public boolean validate(Event event) {
        if (event.getVersion() != 1) { // Gate supports Event of version 1 only
            LOGGER.warn("Event version != 1");
            return false;
        }

        if (event.getTimestamp() < 0) { // Event timestamp should be non-negative
            LOGGER.warn("Event timestamp < 0");
            return false;
        }

        int eventSize = event.getBytes().length;
        if (eventSize > maxEventSize) {
            LOGGER.warn("Event size = {} bytes, more then limit = {}", eventSize, maxEventSize);
            return false;
        }

        return true;
    }

    private static class Props {
        static final Parameter<Integer> MAX_EVENT_SIZE =
                Parameter.integerParameter("max.event.size").
                        withValidator(IntegerValidators.positive()).
                        withDefault(500_000).
                        build();
    }
}
