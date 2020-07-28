package ru.kontur.vostok.hercules.elastic.sink.format;

import org.junit.Test;
import ru.kontur.vostok.hercules.json.DocumentWriter;
import ru.kontur.vostok.hercules.json.format.EventJsonFormatter;
import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.EventBuilder;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.protocol.Vector;
import ru.kontur.vostok.hercules.tags.CommonTags;
import ru.kontur.vostok.hercules.tags.ExceptionTags;
import ru.kontur.vostok.hercules.tags.LogEventTags;
import ru.kontur.vostok.hercules.tags.StackFrameTags;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

/**
 * @author Gregory Koshelev
 */
public class ElasticEventFormatAndWriteTest {
    @Test
    public void shouldFormatAndWriteLogEvent() throws IOException {
        Event event = EventBuilder.create(0, "11203800-63fd-11e8-83e2-3a587d902000").
                tag(CommonTags.PROPERTIES_TAG.getName(), Variant.ofContainer(Container.of("someKey", Variant.ofString("some value")))).
                tag(LogEventTags.EXCEPTION_TAG.getName(), Variant.ofContainer(createException())).
                tag(LogEventTags.MESSAGE_TAG.getName(), Variant.ofString("Test message")).
                build();

        Properties properties = new Properties();
        properties.setProperty("file", "resource://log-event.mapping");
        EventJsonFormatter formatter = new EventJsonFormatter(properties);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        DocumentWriter.writeTo(stream, formatter.format(event));

        assertEquals("{" +
                        "\"@timestamp\":\"1970-01-01T00:00:00.000000000Z\"," +
                        "\"someKey\":\"some value\"," +
                        "\"stackTrace\":\"com.example.test.exceptions.ExceptionClass: Some error of ExceptionClass happened\\n" +
                        "at com.example.test.SomeModule.function(SomeModule.java:100:12)\\n" +
                        "at com.example.test.AnotherModule.function(AnotherModule.java:200:13)\"," +
                        "\"message\":\"Test message\"" +
                        "}",
                stream.toString(StandardCharsets.UTF_8.name())
        );
    }

    private Container createException() {
        return Container.builder()
                .tag(ExceptionTags.TYPE_TAG.getName(), Variant.ofString("com.example.test.exceptions.ExceptionClass"))
                .tag(ExceptionTags.MESSAGE_TAG.getName(), Variant.ofString("Some error of ExceptionClass happened"))
                .tag(ExceptionTags.STACK_FRAMES.getName(), Variant.ofVector(Vector.ofContainers(
                        Container.builder()
                                .tag(StackFrameTags.TYPE_TAG.getName(), Variant.ofString("com.example.test.SomeModule"))
                                .tag(StackFrameTags.FUNCTION_TAG.getName(), Variant.ofString("function"))
                                .tag(StackFrameTags.FILE_TAG.getName(), Variant.ofString("SomeModule.java"))
                                .tag(StackFrameTags.LINE_NUMBER_TAG.getName(), Variant.ofInteger(100))
                                .tag(StackFrameTags.COLUMN_NUMBER_TAG.getName(), Variant.ofShort((short) 12))
                                .build(),
                        Container.builder()
                                .tag(StackFrameTags.TYPE_TAG.getName(), Variant.ofString("com.example.test.AnotherModule"))
                                .tag(StackFrameTags.FUNCTION_TAG.getName(), Variant.ofString("function"))
                                .tag(StackFrameTags.FILE_TAG.getName(), Variant.ofString("AnotherModule.java"))
                                .tag(StackFrameTags.LINE_NUMBER_TAG.getName(), Variant.ofInteger(200))
                                .tag(StackFrameTags.COLUMN_NUMBER_TAG.getName(), Variant.ofShort((short) 13))
                                .build()
                )))
                .build();
    }
}
