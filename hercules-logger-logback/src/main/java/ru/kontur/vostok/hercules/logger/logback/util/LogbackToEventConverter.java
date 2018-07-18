package ru.kontur.vostok.hercules.logger.logback.util;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.protocol.encoder.EventBuilder;
import ru.kontur.vostok.hercules.uuid.UuidGenerator;

import java.util.*;

/**
 * Converter for logback {@link ILoggingEvent logs}
 *
 * @author Daniil Zhenikhov
 */
public class LogbackToEventConverter {
    private static final UuidGenerator GENERATOR = UuidGenerator.getClientInstance();

    public static Event createEvent(ILoggingEvent loggingEvent) {
        EventBuilder eventBuilder = new EventBuilder();

        eventBuilder.setEventId(GENERATOR.next());
        eventBuilder.setVersion(1);

        eventBuilder.setTag("level", Variant.ofString(loggingEvent.getLevel().levelStr));
        eventBuilder.setTag("message", Variant.ofString(loggingEvent.getMessage()));

        if (loggingEvent.getThrowableProxy() != null) {
            eventBuilder.setTag("exceptions", Variant.ofContainerArray(getArrayExceptionContainer(loggingEvent.getThrowableProxy())));
        }

        return eventBuilder.build();
    }

    /**
     * Form {@link Container Hercules Container} from throwable
     *
     * @param throwableProxy Exception by which the container has been built
     * @return Exception Container
     */
    private static Container getExceptionContainer(IThrowableProxy throwableProxy) {
        Map<String, Variant> result = new HashMap<>();

        result.put("type", Variant.ofString(throwableProxy.getClassName()));
        result.put("message", Variant.ofString(throwableProxy.getMessage()));

        if (throwableProxy.getStackTraceElementProxyArray().length > 0) {
            String className = throwableProxy
                    .getStackTraceElementProxyArray()[0]
                    .getStackTraceElement()
                    .getClassName();
            result.put("module", Variant.ofString(className));
        }

        Container[] containers = Arrays
                .stream(throwableProxy.getStackTraceElementProxyArray())
                .map(StackTraceElementProxy::getStackTraceElement)
                .map(LogbackToEventConverter::getStacktraceContainer)
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
     * @param throwableProxy Exception by which the array of containers has been built
     * @return array of exception container
     */
    private static Container[] getArrayExceptionContainer(IThrowableProxy throwableProxy) {
        List<Container> containers = new ArrayList<>();
        IThrowableProxy currentThrowableProxy = throwableProxy;

        while (currentThrowableProxy != null) {
            containers.add(getExceptionContainer(currentThrowableProxy));
            currentThrowableProxy = currentThrowableProxy.getCause();
        }

        Container[] containersArray = new Container[containers.size()];
        return containers.toArray(containersArray);
    }
}

