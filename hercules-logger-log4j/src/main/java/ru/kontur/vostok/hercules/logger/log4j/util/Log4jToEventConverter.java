package ru.kontur.vostok.hercules.logger.log4j.util;

import org.apache.logging.log4j.core.LogEvent;
import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.protocol.encoder.EventBuilder;
import ru.kontur.vostok.hercules.uuid.UuidGenerator;

import java.util.*;

/**
 * Converter for log4j {@link LogEvent logs}
 *
 * @author Daniil Zhenikhov
 */
public class Log4jToEventConverter {
    private final static UuidGenerator GENERATOR = UuidGenerator.getClientInstance();

    public static Event createEvent(LogEvent logEvent) {
        EventBuilder eventBuilder = new EventBuilder();

        eventBuilder.setEventId(GENERATOR.next());
        eventBuilder.setVersion(1);

        eventBuilder.setTag("level", Variant.ofString(logEvent.getLevel().toString()));
        eventBuilder.setTag("message", Variant.ofString(logEvent.getMessage().getFormattedMessage()));

        if (logEvent.getThrown() != null) {
            eventBuilder.setTag("exceptions", Variant.ofContainerArray(getArrayExceptionContainer(logEvent.getThrown())));
        }

        return eventBuilder.build();
    }

    /**
     * Form {@link Container Hercules Container} from throwable
     *
     * @param throwable Exception by which the container has been built
     * @return Exception Container
     */
    private static Container getExceptionContainer(Throwable throwable) {
        Map<String, Variant> result = new HashMap<>();

        result.put("type", Variant.ofString(throwable.getClass().getName()));
        result.put("message", Variant.ofString(throwable.getMessage()));

        if (throwable.getStackTrace().length > 0) {
            result.put("module", Variant.ofString(throwable.getStackTrace()[0].getClassName()));
        }

        Container[] containers = Arrays
                .stream(throwable.getStackTrace())
                .map(Log4jToEventConverter::getStacktraceContainer)
                .toArray(Container[]::new);
        result.put("stacktrace", Variant.ofContainerArray(containers));

        return new Container(result);
    }

    /**
     * Form {@link Container Hercules Container} from stack trace element
     *
     * @param element stacktrace element by which the container has been built
     * @return Stacktrace element container
     */
    private static Container getStacktraceContainer(StackTraceElement element) {
        Map<String, Variant> result = new HashMap<>();

        result.put("file", Variant.ofString(element.getFileName()));
        result.put("line", Variant.ofInteger(element.getLineNumber()));
        result.put("source", Variant.ofString(element.getClassName()));
        result.put("function", Variant.ofString(element.getMethodName()));

        return new Container(result);
    }

    /**
     * Form array of exception containers
     *
     * @param throwable Exception by which the array of containers has been built
     * @return array of exception container
     */
    private static Container[] getArrayExceptionContainer(Throwable throwable) {
        List<Container> containers = new ArrayList<>();
        Throwable currentThrowable = throwable;

        while (currentThrowable != null) {
            containers.add(getExceptionContainer(currentThrowable));
            currentThrowable = currentThrowable.getCause();
        }

        Container[] containersArray = new Container[containers.size()];
        return containers.toArray(containersArray);
    }
}
