package ru.kontur.vostok.hercules.logger.log4j2.util;

import org.apache.logging.log4j.core.LogEvent;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.protocol.encoder.EventBuilder;
import ru.kontur.vostok.hercules.uuid.UuidGenerator;

import java.util.Arrays;

public class Log4jToEventConverter {
    private final static UuidGenerator GENERATOR = UuidGenerator.getClientInstance();

    public static Event createEvent(LogEvent logEvent) {
        EventBuilder eventBuilder = new EventBuilder();

        eventBuilder.setEventId(GENERATOR.next());
        eventBuilder.setVersion(1);
        eventBuilder.setTag("level", Variant.ofString(logEvent.getLevel().toString()));
        eventBuilder.setTag("message", Variant.ofString(logEvent.getMessage().getFormattedMessage()));

        //TODO: Do we need to save the context of log?

        if (logEvent.getThrown() != null) {
            String[] stackTrace = Arrays
                    .stream(logEvent.getThrown().getStackTrace())
                    .map(StackTraceElement::toString)
                    .toArray(String[]::new);

            eventBuilder.setTag("stacktrace", Variant.ofStringArray(stackTrace));
        }


        return eventBuilder.build();
    }
}
