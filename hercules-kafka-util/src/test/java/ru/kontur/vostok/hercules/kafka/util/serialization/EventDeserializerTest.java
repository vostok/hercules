package ru.kontur.vostok.hercules.kafka.util.serialization;

import org.junit.Test;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.protocol.util.EventBuilder;
import ru.kontur.vostok.hercules.uuid.UuidGenerator;

import java.util.Collections;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * @author Gregory Koshelev
 */
public class EventDeserializerTest {

    private static final String TOPIC_STUB = null;

    @Test
    public void shouldParseNoTags() {
        EventDeserializer eventDeserializer = EventDeserializer.parseNoTags();

        Event deserialized = eventDeserializer.deserialize(TOPIC_STUB, createEvent().getBytes());

        assertEquals(0, deserialized.getPayload().size());
    }

    @Test
    public void shouldParseAllTags() {
        EventDeserializer eventDeserializer = EventDeserializer.parseAllTags();

        Event deserialized = eventDeserializer.deserialize(TOPIC_STUB, createEvent().getBytes());

        assertEquals(2, deserialized.getPayload().size());
        assertArrayEquals("tag content".getBytes(), (byte[]) deserialized.getPayload().get("string-tag").getValue());
        assertEquals(123, (int) deserialized.getPayload().get("int-tag").getValue());
    }

    @Test
    public void shouldParseSelectedTags() {
        EventDeserializer eventDeserializer = EventDeserializer.parseTags(Collections.singleton("int-tag"));

        Event deserialized = eventDeserializer.deserialize(TOPIC_STUB, createEvent().getBytes());

        assertEquals(1, deserialized.getPayload().size());
        assertEquals(123, (int) deserialized.getPayload().get("int-tag").getValue());
    }

    private static Event createEvent() {
        return EventBuilder.create(0, UuidGenerator.getClientInstance().next())
                .tag("string-tag", Variant.ofString("tag content"))
                .tag("int-tag", Variant.ofInteger(123))
                .build();
    }
}
