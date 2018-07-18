import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.SimpleMessage;
import org.junit.Test;
import ru.kontur.vostok.hercules.logger.log4j.util.Log4jToEventConverter;
import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.HerculesProtocolAssert;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.protocol.encoder.EventBuilder;
import ru.kontur.vostok.hercules.uuid.UuidGenerator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Daniil Zhenikhov
 */
public class Log4jToEventConverterTest {
    private static final UuidGenerator GENERATOR = UuidGenerator.getClientInstance();
    private static final String TEST_LOG_MESSAGE = "TEST_LOG_MESSAGE";
    private static final String TEST_EXCEPTION_MESSAGE = "TEST_EXCEPTION_MESSAGE";
    private static final String DECLARING_CLASS = "DECLARING_CLASS";
    private static final String METHOD_NAME = "METHOD_NAME";
    private static final String FILENAME = "FILENAME";

    private final Event eventWithoutException = buildEvent(createContextWithoutException());
    private final Event eventWithException = buildEvent(createContextWithException());
    private final Event eventWithTwoExceptions = buildEvent(createContextWithTwoExceptions());

    private final LogEvent logEventWithoutException = createLogEvent();
    private final LogEvent logEventWithException = createLogEvent(createThrowable(0));
    private final LogEvent logEventWithTwoException = createLogEvent(createThrowable(0, createThrowable(1)));

    @Test
    public void shouldConvertWithoutException() {
        Event actual = Log4jToEventConverter.createEvent(logEventWithoutException);

        HerculesProtocolAssert.assertEquals(eventWithoutException, actual, false, false);
    }

    @Test
    public void shouldConvertWithExceptionWithoutInnerException() {
        Event actual = Log4jToEventConverter.createEvent(logEventWithException);

        HerculesProtocolAssert.assertEquals(eventWithException, actual, false, false);
    }

    @Test
    public void shouldConvertWithExceptionAndWithInnerException() {
        Event actual = Log4jToEventConverter.createEvent(logEventWithTwoException);

        HerculesProtocolAssert.assertEquals(eventWithTwoExceptions, actual, false, false);
    }

    private LogEvent createLogEvent(Throwable throwable) {
        Log4jLogEvent.Builder builder = Log4jLogEvent
                .newBuilder()
                .setLevel(Level.INFO)
                .setMessage(new SimpleMessage(TEST_LOG_MESSAGE));

        if (throwable != null) {
            builder.setThrown(throwable);
        }

        return builder.build();
    }

    private LogEvent createLogEvent() {
        return createLogEvent(null);
    }

    private Map<String, Variant> createContextWithoutException() {
        Map<String, Variant> map = new HashMap<>();
        map.put("level", Variant.ofString(Level.INFO.toString()));
        map.put("message", Variant.ofString(TEST_LOG_MESSAGE));

        return map;
    }

    private Map<String, Variant> createContextWithException() {
        Map<String, Variant> map = createContextWithoutException();
        Container[] containers = new Container[]{createContainerFromThrowable(0)};

        map.put("exceptions", Variant.ofContainerArray(containers));

        return map;
    }

    private Map<String, Variant> createContextWithTwoExceptions() {
        Map<String, Variant> map = createContextWithoutException();
        Container[] containers = new Container[]{
                createContainerFromThrowable(0),
                createContainerFromThrowable(1)
        };

        map.put("exceptions", Variant.ofContainerArray(containers));
        return map;

    }

    private Container createContainerFromThrowable(int index) {
        Map<String, Variant> map = new HashMap<>();
        map.put("type", Variant.ofString(Throwable.class.getName()));
        map.put("message", Variant.ofString(TEST_EXCEPTION_MESSAGE + index));

        StackTraceElement[] stackTraceElements = createStackTraceElementArray(index, 2);
        Container[] containers = new Container[2];
        for (int i = 0; i < 2; i++) {
            containers[i] = createContainerFromStackTraceElement(index, i);
        }

        map.put("module", Variant.ofString(stackTraceElements[0].getClassName()));
        map.put("stacktrace", Variant.ofContainerArray(containers));

        return new Container(map);
    }

    private Container createContainerFromStackTraceElement(int index, int row) {
        Map<String, Variant> map = new HashMap<>();

        map.put("file", Variant.ofString(form(FILENAME, index, row)));
        map.put("line", Variant.ofInteger(index));
        map.put("source", Variant.ofString(form(DECLARING_CLASS, index, row)));
        map.put("function", Variant.ofString(form(METHOD_NAME, index, row)));

        return new Container(map);
    }

    private Event buildEvent(Map<String, Variant> map) {
        EventBuilder eventBuilder = new EventBuilder();
        eventBuilder.setEventId(GENERATOR.next());
        eventBuilder.setVersion(1);

        map.forEach(eventBuilder::setTag);

        return eventBuilder.build();
    }

    private Throwable createThrowable(int index) {
        return createThrowable(index, null);
    }

    private Throwable createThrowable(int index, Throwable cause) {
        Throwable throwable;
        if (cause != null) {
            throwable = new Throwable(TEST_EXCEPTION_MESSAGE + index, cause);
        } else {
            throwable = new Throwable(TEST_EXCEPTION_MESSAGE + index);
        }
        throwable.setStackTrace(createStackTraceElementArray(index, 2));

        return throwable;
    }

    private String form(String name, int index, int row) {
        return String.format("%s-%d-%d", name, index, row);
    }

    private StackTraceElement[] createStackTraceElementArray(int index, int count) {
        List<StackTraceElement> elements = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            elements.add(createStackTraceElement(index, i));
        }

        StackTraceElement[] elementsArray = new StackTraceElement[elements.size()];
        return elements.toArray(elementsArray);
    }

    private StackTraceElement createStackTraceElement(int index, int row) {
        return new StackTraceElement(
                form(DECLARING_CLASS, index, row),
                form(METHOD_NAME, index, row),
                form(FILENAME, index, row),
                index
        );
    }
}
