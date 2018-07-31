package ru.kontur.vostok.hercules.elasticsearch.sink;

import org.junit.Test;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.protocol.encoder.EventBuilder;

import java.io.ByteArrayOutputStream;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class IndexToElasticJsonWriterTest {


    @Test
    public void shouldWriteIndexIfEventHasIndexTag() throws Exception {
        EventBuilder eventBuilder = new EventBuilder();
        eventBuilder.setEventId(UUID.fromString("00000000-0000-1000-994f-8fcf383f0000"));
        eventBuilder.setTag("index", Variant.ofString("just-some-index-value"));
        Event event = eventBuilder.build();

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        IndexToElasticJsonWriter.tryWriteIndex(stream, event);

        assertEquals("{\"index\":{\"_index\":\"just-some-index-value\",\"_type\":\"_doc\",\"_id\":\"00000000-0000-1000-994f-8fcf383f0000\"}}", stream.toString());
    }

    @Test
    public void shouldWriteIndexIfEventHasProjectAndEnvTags() throws Exception {
        EventBuilder eventBuilder = new EventBuilder();
        eventBuilder.setEventId(UUID.fromString("00000000-0000-1000-994f-8fcf383f0000"));
        eventBuilder.setTag("project", Variant.ofString("awesome-project"));
        eventBuilder.setTag("env", Variant.ofString("production"));
        Event event = eventBuilder.build();

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        IndexToElasticJsonWriter.tryWriteIndex(stream, event);

        assertEquals("{\"index\":{\"_index\":\"awesome-project-production-1582.10.15\",\"_type\":\"_doc\",\"_id\":\"00000000-0000-1000-994f-8fcf383f0000\"}}", stream.toString());
    }

    @Test
    public void shouldReturnFalseIfNoSuitableTags() throws Exception {
        EventBuilder eventBuilder = new EventBuilder();
        eventBuilder.setEventId(UUID.fromString("00000000-0000-1000-994f-8fcf383f0000"));
        Event event = eventBuilder.build();

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        boolean result = IndexToElasticJsonWriter.tryWriteIndex(stream, event);

        assertEquals("", stream.toString());
        assertFalse(result);
    }
}
