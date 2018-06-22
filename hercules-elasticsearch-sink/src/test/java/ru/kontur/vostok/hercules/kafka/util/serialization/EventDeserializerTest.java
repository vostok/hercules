package ru.kontur.vostok.hercules.kafka.util.serialization;

import org.junit.Test;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.protocol.encoder.EventBuilder;

import java.util.Collections;

import static org.junit.Assert.*;

public class EventDeserializerTest {

    private static final String TOPIC_STUB = null;

    @Test
    public void shouldParseNoTags() {
        EventDeserializer eventDeserializer = EventDeserializer.parseNoTags();

        Event deserialized = eventDeserializer.deserialize(TOPIC_STUB, createEvent().getBytes());

        assertEquals(0, deserialized.getTags().size());
    }

    @Test
    public void shouldParseAllTags() {
        EventDeserializer eventDeserializer = EventDeserializer.parseAllTags();

        Event deserialized = eventDeserializer.deserialize(TOPIC_STUB, createEvent().getBytes());

        assertEquals(2, deserialized.getTags().size());
        assertArrayEquals("tag content".getBytes(), (byte[]) deserialized.getTags().get("string-tag").getValue());
        assertEquals(123, (int) deserialized.getTags().get("int-tag").getValue());
    }

    @Test
    public void shouldParseSelectedTags() {
        EventDeserializer eventDeserializer = EventDeserializer.parseTags(Collections.singleton("int-tag"));

        Event deserialized = eventDeserializer.deserialize(TOPIC_STUB, createEvent().getBytes());

        assertEquals(1, deserialized.getTags().size());
        assertEquals(123, (int) deserialized.getTags().get("int-tag").getValue());
    }

    private static Event createEvent() {
        EventBuilder builder = new EventBuilder();

        builder.setVersion(1);
        builder.setTimestamp(0);

        builder.setTag("string-tag", Variant.ofString("tag content"));
        builder.setTag("int-tag", Variant.ofInteger(123));

        return builder.build();
    }
}
