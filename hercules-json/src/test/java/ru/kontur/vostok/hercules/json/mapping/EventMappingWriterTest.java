package ru.kontur.vostok.hercules.json.mapping;

import org.junit.Test;
import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.EventBuilder;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.util.time.TimeUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

/**
 * @author Gregory Koshelev
 */
public class EventMappingWriterTest {
    @Test
    public void shouldMapEventTimestamp() throws IOException {
        Event event = EventBuilder.create(TimeUtil.unixTimeToUnixTicks(3600), "11203800-63fd-11e8-83e2-3a587d902000").
                build();

        Properties properties = new Properties();
        properties.setProperty(EventMappingWriter.Props.TIMESTAMP_ENABLE.name(), "true");
        properties.setProperty(EventMappingWriter.Props.TIMESTAMP_FIELD.name(), "@timestamp");
        properties.setProperty(EventMappingWriter.Props.TIMESTAMP_FORMAT.name(), "yyyy-MM-dd'T'HH:mm:ss.nnnnnnnnnX");
        properties.setProperty("file", "resource://empty.mapping");
        EventMappingWriter writer = new EventMappingWriter(properties);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        writer.write(stream, event);

        assertEquals("{" +
                        "\"@timestamp\":\"1970-01-01T01:00:00.000000000Z\"" +
                        "}",
                stream.toString(StandardCharsets.UTF_8.name())
        );
    }

    @Test
    public void shouldMoveTags() throws IOException {
        Event event = EventBuilder.create(TimeUtil.unixTimeToUnixTicks(3600), "11203800-63fd-11e8-83e2-3a587d902000").
                tag("source", Variant.ofContainer(Container.of("field", Variant.ofString("value")))).
                build();

        Properties properties = new Properties();
        properties.setProperty(EventMappingWriter.Props.TIMESTAMP_ENABLE.name(), "false");
        properties.setProperty("file", "resource://move.mapping");
        EventMappingWriter writer = new EventMappingWriter(properties);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        writer.write(stream, event);

        assertEquals("{" +
                        "\"destination\":{" +
                        "\"field\":\"value\"" +
                        "}" +
                        "}",
                stream.toString(StandardCharsets.UTF_8.name())
        );
    }

    @Test
    public void shouldTransformTags() throws IOException {
        Event event = EventBuilder.create(TimeUtil.unixTimeToUnixTicks(3600), "11203800-63fd-11e8-83e2-3a587d902000").
                tag("integerTag", Variant.ofInteger(123)).
                build();

        Properties properties = new Properties();
        properties.setProperty("file", "resource://transform.mapping");
        EventMappingWriter writer = new EventMappingWriter(properties);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        writer.write(stream, event);

        assertEquals("{" +
                        "\"@timestamp\":\"1970-01-01T01:00:00.000000000Z\"," +
                        "\"stringField\":\"123\"" +
                        "}",
                stream.toString(StandardCharsets.UTF_8.name())
        );
    }
}
