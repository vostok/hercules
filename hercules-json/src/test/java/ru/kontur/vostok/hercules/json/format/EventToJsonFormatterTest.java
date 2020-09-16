package ru.kontur.vostok.hercules.json.format;

import org.junit.Test;
import ru.kontur.vostok.hercules.json.DocumentWriter;
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
public class EventToJsonFormatterTest {
    @Test
    public void shouldMapEventTimestamp() throws IOException {
        Event event = EventBuilder.create(TimeUtil.unixTimeToUnixTicks(3600), "11203800-63fd-11e8-83e2-3a587d902000").
                build();

        Properties properties = new Properties();
        properties.setProperty(EventToJsonFormatter.Props.TIMESTAMP_ENABLE.name(), "true");
        properties.setProperty(EventToJsonFormatter.Props.TIMESTAMP_FIELD.name(), "@timestamp");
        properties.setProperty(EventToJsonFormatter.Props.TIMESTAMP_FORMAT.name(), "yyyy-MM-dd'T'HH:mm:ss.nnnnnnnnnX");
        properties.setProperty(EventToJsonFormatter.Props.FILE.name(), "resource://empty.mapping");
        EventToJsonFormatter formatter = new EventToJsonFormatter(properties);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        DocumentWriter.writeTo(stream, formatter.format(event));

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
                tag("sourceWithExcepted",
                        Variant.ofContainer(
                                Container.builder().
                                        tag("shouldExceptOne", Variant.ofInteger(1)).
                                        tag("shouldExceptTwo", Variant.ofInteger(2)).
                                        tag("notExcepted", Variant.ofString("42")).
                                        build())).
                build();

        Properties properties = new Properties();
        properties.setProperty(EventToJsonFormatter.Props.TIMESTAMP_ENABLE.name(), "false");
        properties.setProperty(EventToJsonFormatter.Props.FILE.name(), "resource://move.mapping");
        EventToJsonFormatter formatter = new EventToJsonFormatter(properties);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        DocumentWriter.writeTo(stream, formatter.format(event));

        assertEquals("{" +
                        "\"destination\":{" +
                        "\"field\":\"value\"" +
                        "}," +
                        "\"destinationWithoutExcepted\":{" +
                        "\"notExcepted\":\"42\"" +
                        "}" +
                        "}",
                stream.toString(StandardCharsets.UTF_8.name())
        );
    }

    @Test
    public void shouldTransformTags() throws IOException {
        Event event = EventBuilder.create(TimeUtil.unixTimeToUnixTicks(3600), "11203800-63fd-11e8-83e2-3a587d902000").
                tag("integerTag", Variant.ofInteger(123)).
                tag("tag", Variant.ofString("value")).
                build();

        Properties properties = new Properties();
        properties.setProperty(EventToJsonFormatter.Props.FILE.name(), "resource://transform.mapping");
        EventToJsonFormatter formatter = new EventToJsonFormatter(properties);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        DocumentWriter.writeTo(stream, formatter.format(event));

        assertEquals("{" +
                        "\"@timestamp\":\"1970-01-01T01:00:00.000000000Z\"," +
                        "\"stringField\":\"123\"," +
                        "\"sub\":{" +
                        "\"field\":\"value\"" +
                        "}" +
                        "}",
                stream.toString(StandardCharsets.UTF_8.name())
        );
    }

    @Test
    public void shouldMapTagMultipleTimes() throws IOException {
        Event event = EventBuilder.create(TimeUtil.unixTimeToUnixTicks(3600), "11203800-63fd-11e8-83e2-3a587d902000").
                tag("integerTag", Variant.ofInteger(123)).
                build();

        Properties properties = new Properties();
        properties.setProperty(EventToJsonFormatter.Props.FILE.name(), "resource://tag-to-multiple-fields.mapping");
        EventToJsonFormatter formatter = new EventToJsonFormatter(properties);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        DocumentWriter.writeTo(stream, formatter.format(event));

        assertEquals("{" +
                        "\"@timestamp\":\"1970-01-01T01:00:00.000000000Z\"," +
                        "\"integerField\":123," +
                        "\"stringField\":\"123\"" +
                        "}",
                stream.toString(StandardCharsets.UTF_8.name())
        );
    }

    @Test
    public void shouldCombineTags() throws IOException {
        Event event = EventBuilder.create(TimeUtil.unixTimeToUnixTicks(3600), "11203800-63fd-11e8-83e2-3a587d902000").
                tag("beginTimestampUtc", Variant.ofLong(15954978501499766L)).
                tag("endTimestampUtc", Variant.ofLong(15954978501533487L)).
                tag("timestampOffset", Variant.ofLong(108000000000L)).
                build();

        Properties properties = new Properties();
        properties.setProperty(EventToJsonFormatter.Props.TIMESTAMP_ENABLE.name(), "false");
        properties.setProperty(EventToJsonFormatter.Props.FILE.name(), "resource://combine.mapping");
        EventToJsonFormatter formatter = new EventToJsonFormatter(properties);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        DocumentWriter.writeTo(stream, formatter.format(event));

        assertEquals("{" +
                        "\"dateTime\":\"2020-07-23T12:50:50.149976600+03:00\"," +
                        "\"latencyMs\":3," +
                        "\"latency\":\"3.372 milliseconds\"" +
                        "}",
                stream.toString(StandardCharsets.UTF_8.name())
        );
    }
}
