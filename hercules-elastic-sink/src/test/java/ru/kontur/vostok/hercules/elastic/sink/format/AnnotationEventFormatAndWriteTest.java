package ru.kontur.vostok.hercules.elastic.sink.format;

import org.junit.Test;
import ru.kontur.vostok.hercules.json.DocumentWriter;
import ru.kontur.vostok.hercules.json.format.EventToJsonFormatter;
import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.EventBuilder;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.protocol.Vector;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

/**
 * @author Gregory Koshelev
 */
public class AnnotationEventFormatAndWriteTest {
    @Test
    public void shouldFormatAndWriteAnnotationEvent() throws IOException {
        Event event = EventBuilder.create(0, "11203800-63fd-11e8-83e2-3a587d902000").
                tag("description", Variant.ofString("This is the annotation")).
                tag("tags", Variant.ofVector(Vector.ofContainers(
                        Container.builder().tag("key", Variant.ofString("environment")).tag("value", Variant.ofString("staging")).build(),
                        Container.builder().tag("key", Variant.ofString("hostname")).tag("value", Variant.ofString("localhost")).build()))).
                build();

        Properties properties = new Properties();
        properties.setProperty("file", "resource://annotation-event.mapping");
        EventToJsonFormatter formatter = new EventToJsonFormatter(properties);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        DocumentWriter.writeTo(stream, formatter.format(event));

        assertEquals("{" +
                        "\"@timestamp\":\"1970-01-01T00:00:00.000000000Z\"," +
                        "\"environment\":\"staging\"," +
                        "\"hostname\":\"localhost\"," +
                        "\"tags\":\"environment=staging,hostname=localhost,\"," +
                        "\"description\":\"This is the annotation\"" +
                        "}",
                stream.toString(StandardCharsets.UTF_8.name())
        );
    }
}
