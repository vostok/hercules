package ru.kontur.vostok.hercules.logger.logback.util;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.protocol.encoder.EventBuilder;
import ru.kontur.vostok.hercules.uuid.UuidGenerator;

import java.util.Arrays;

public class EventUtil {
    private final static UuidGenerator GENERATOR = UuidGenerator.getClientInstance();

    public static Event createEvent(ILoggingEvent loggingEvent) {
        EventBuilder eventBuilder = new EventBuilder();

        eventBuilder.setEventId(GENERATOR.next());
        eventBuilder.setVersion(1);
        eventBuilder.setTag("level", Variant.ofString(loggingEvent.getLevel().levelStr));
        eventBuilder.setTag("message", Variant.ofString(loggingEvent.getMessage()));

        //TODO: Do we need to save the context of log?
//        loggingEvent
//                .getMDCPropertyMap()
//                .forEach((key, value) -> eventBuilder.setTag(key, Variant.ofString(value)));

        if (loggingEvent.hasCallerData()) {
            eventBuilder.setTag(
                    "stacktrace",
                    Variant.ofStringArray(Arrays.stream(loggingEvent.getCallerData())
                            .map(StackTraceElement::toString)
                            .toArray(String[]::new))
            );
        }

        return eventBuilder.build();
    }
}

