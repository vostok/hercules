package ru.kontur.vostok.hercules.logger.log4j.util;

import org.apache.logging.log4j.core.LogEvent;
import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.protocol.encoder.EventBuilder;
import ru.kontur.vostok.hercules.uuid.UuidGenerator;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class Log4jToEventConverter {
    private final static UuidGenerator GENERATOR = UuidGenerator.getClientInstance();

    public static Event createEvent(LogEvent logEvent) {
        EventBuilder eventBuilder = new EventBuilder();

        eventBuilder.setEventId(GENERATOR.next());
        eventBuilder.setVersion(1);
        eventBuilder.setTag("level", Variant.ofString(logEvent.getLevel().toString()));
        eventBuilder.setTag("message", Variant.ofString(logEvent.getMessage().getFormattedMessage()));

        //TODO: add exception


        return eventBuilder.build();
    }

    private Container getExceptionContainer(Throwable throwable) {
        Map<String, Variant> result = new HashMap<>();

        result.put("type", Variant.ofString(throwable.getClass().getName()));
        result.put("message", Variant.ofString(throwable.getMessage()));
        if (throwable.getStackTrace().length > 0) {
            result.put("module", Variant.ofString(throwable.getStackTrace()[0].getClassName()));
        }
        //TODO: wait for array container
//        result.put("stacktrace", Variant.ofContainer());

        return new Container(result);
    }

    private Container getStacktraceContainer(StackTraceElement element) {
        Map<String, Variant> result = new HashMap<>();

        result.put("file", Variant.ofString(element.getFileName()));
        result.put("line", Variant.ofInteger(element.getLineNumber()));
        result.put("source", Variant.ofString(element.getClassName()));
        result.put("function", Variant.ofString(element.getMethodName()));

        return new Container(result);
    }
}
