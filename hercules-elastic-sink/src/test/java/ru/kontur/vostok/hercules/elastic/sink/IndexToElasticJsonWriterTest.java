package ru.kontur.vostok.hercules.elastic.sink;

import org.junit.Test;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.protocol.util.ContainerBuilder;
import ru.kontur.vostok.hercules.protocol.util.EventBuilder;
import ru.kontur.vostok.hercules.protocol.util.EventUtil;
import ru.kontur.vostok.hercules.tags.CommonTags;
import ru.kontur.vostok.hercules.util.time.TimeUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class IndexToElasticJsonWriterTest {

    @Test
    public void shouldWriteIndexIfEventHasIndexTag() throws Exception {
        final Event event = EventBuilder.create(0, "00000000-0000-1000-994f-8fcf383f0000") //TODO: fix me!
                .tag("properties", Variant.ofContainer(ContainerBuilder.create()
                        .tag("elk-index", Variant.ofString("just-some-index-value"))
                        .build()
                )).build();

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        eventProcess(stream, event);
        assertEquals(
                "{" +
                        "\"index\":{" +
                        "\"_index\":\"just-some-index-value-1970.01.01\"," +
                        "\"_type\":\"LogEvent\"," +
                        "\"_id\":\"AAAAAAAAAAAAAAAAAAAQAJlPj884PwAA\"" +
                        "}" +
                        "}",
                stream.toString()
        );
    }

    @Test
    public void shouldWriteIndexIfEventHasProjectAndEnvTags() throws Exception {
        final Event event = EventBuilder.create(TimeUtil.UNIX_EPOCH, "00000000-0000-1000-994f-8fcf383f0000")
                .tag("properties", Variant.ofContainer(ContainerBuilder.create()
                        .tag("project", Variant.ofString("awesome-project"))
                        .tag("environment", Variant.ofString("production"))
                        .build()
                ))
                .build();

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        eventProcess(stream, event);

        assertEquals(
                "{" +
                        "\"index\":{" +
                        "\"_index\":\"awesome-project-production-1970.01.01\"," +
                        "\"_type\":\"LogEvent\"," +
                        "\"_id\":\"AAAAAAAAAAAAAAAAAAAQAJlPj884PwAA\"" +
                        "}" +
                        "}",
                stream.toString()
        );
    }

    @Test
    public void shouldUseElkScopeForIndexName() throws Exception {
        final Event event = EventBuilder.create(TimeUtil.UNIX_EPOCH, "00000000-0000-1000-994f-8fcf383f0000")
                .tag("properties", Variant.ofContainer(ContainerBuilder.create()
                        .tag("project", Variant.ofString("awesome-project"))
                        .tag(CommonTags.APPLICATION_TAG, Variant.ofString("app"))
                        .tag("environment", Variant.ofString("production"))
                        .build()
                ))
                .build();

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        eventProcess(stream, event);

        assertEquals(
                "{" +
                        "\"index\":{" +
                        "\"_index\":\"awesome-project-app-production-1970.01.01\"," +
                        "\"_type\":\"LogEvent\"," +
                        "\"_id\":\"AAAAAAAAAAAAAAAAAAAQAJlPj884PwAA\"" +
                        "}" +
                        "}",
                stream.toString()
        );
    }

    @Test
    public void shouldUseSubprojectInsteadOfApplicationForIndexName() throws IOException {
        final Event event = EventBuilder.create(TimeUtil.UNIX_EPOCH, "00000000-0000-1000-994f-8fcf383f0000")
                .tag("properties", Variant.ofContainer(ContainerBuilder.create()
                        .tag("project", Variant.ofString("awesome-project"))
                        .tag(CommonTags.APPLICATION_TAG, Variant.ofString("app"))
                        .tag(CommonTags.SUBPROJECT_TAG, Variant.ofString("subproject"))
                        .tag("environment", Variant.ofString("production"))
                        .build()
                ))
                .build();

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        eventProcess(stream, event);

        assertEquals(
                "{" +
                        "\"index\":{" +
                        "\"_index\":\"awesome-project-subproject-production-1970.01.01\"," +
                        "\"_type\":\"LogEvent\"," +
                        "\"_id\":\"AAAAAAAAAAAAAAAAAAAQAJlPj884PwAA\"" +
                        "}" +
                        "}",
                stream.toString()
        );
    }

    @Test
    public void shouldReturnFalseIfNoSuitableTags() {
        final Event event = EventBuilder.create(0, "00000000-0000-1000-994f-8fcf383f0000") //TODO: fix me!
                .build();
        Optional<String> result = IndexToElasticJsonWriter.extractIndex(event);
        assertFalse(result.isPresent());
    }

    private void eventProcess(ByteArrayOutputStream stream, Event event) throws IOException {
        String index = IndexToElasticJsonWriter.extractIndex(event).orElseThrow(NullPointerException::new);
        String eventId = Optional.ofNullable(EventUtil.extractStringId(event)).orElseThrow(NullPointerException::new);
        IndexToElasticJsonWriter.writeIndex(stream, index, eventId);
    }
}
