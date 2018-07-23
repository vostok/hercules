package ru.kontur.vostok.hercules.kafka.util.serialization;

import org.junit.Test;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.protocol.encoder.EventBuilder;
import ru.kontur.vostok.hercules.uuid.UuidGenerator;

import java.util.Collections;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class EventDeserializerTest {

    private static final String TOPIC_STUB = null;

    @Test
    public void shouldParseNoTags() {
        EventDeserializer eventDeserializer = EventDeserializer.parseNoTags();

        Event deserialized = eventDeserializer.deserialize(TOPIC_STUB, createEvent().getBytes());

        assertEquals(0, deserialized.getTagCount());
    }

    @Test
    public void shouldParseAllTags() {
        EventDeserializer eventDeserializer = EventDeserializer.parseAllTags();

        Event deserialized = eventDeserializer.deserialize(TOPIC_STUB, createEvent().getBytes());

        assertEquals(2, deserialized.getTagCount());
        assertArrayEquals("tag content".getBytes(), (byte[]) deserialized.getTag("string-tag").getValue());
        assertEquals(123, (int) deserialized.getTag("int-tag").getValue());
    }

    @Test
    public void shouldParseSelectedTags() {
        EventDeserializer eventDeserializer = EventDeserializer.parseTags(Collections.singleton("int-tag"));

        Event deserialized = eventDeserializer.deserialize(TOPIC_STUB, createEvent().getBytes());

        assertEquals(1, deserialized.getTagCount());
        assertEquals(123, (int) deserialized.getTag("int-tag").getValue());
    }

    private static Event createEvent() {
        EventBuilder builder = new EventBuilder();

        builder.setVersion(1);
        builder.setEventId(UuidGenerator.getClientInstance().next());

        builder.setTag("string-tag", Variant.ofString("tag content"));
        builder.setTag("int-tag", Variant.ofInteger(123));

        return builder.build();
    }
}
