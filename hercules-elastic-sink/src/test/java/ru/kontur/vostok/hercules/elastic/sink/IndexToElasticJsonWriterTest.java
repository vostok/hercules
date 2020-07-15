package ru.kontur.vostok.hercules.elastic.sink;

import org.junit.Test;
import ru.kontur.vostok.hercules.elastic.sink.index.IndexPolicy;
import ru.kontur.vostok.hercules.elastic.sink.index.LogEventIndexResolver;
import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.EventBuilder;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.protocol.util.EventUtil;
import ru.kontur.vostok.hercules.tags.CommonTags;
import ru.kontur.vostok.hercules.util.time.TimeUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Optional;

import static org.junit.Assert.assertEquals;

public class IndexToElasticJsonWriterTest {//FIXME: Rewrite and move index resolving tests to IndexResolverTest.

    @Test
    public void shouldWriteIndexIfEventHasIndexTag() throws Exception {
        final Event event = EventBuilder.create(0, "00000000-0000-1000-994f-8fcf383f0000") //TODO: fix me!
                .tag("properties", Variant.ofContainer(Container.of("elk-index", Variant.ofString("just-some-index-value")))).build();

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
                .tag("properties", Variant.ofContainer(Container.builder()
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
    public void shouldUseSubprojectForIndexName() throws Exception {
        final Event event = EventBuilder.create(TimeUtil.UNIX_EPOCH, "00000000-0000-1000-994f-8fcf383f0000")
                .tag("properties", Variant.ofContainer(Container.builder()
                        .tag("project", Variant.ofString("awesome-project"))
                        .tag(CommonTags.SUBPROJECT_TAG.getName(), Variant.ofString("subproject"))
                        .tag("environment", Variant.ofString("production"))
                        .build()
                ))
                .build();

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        eventProcess(stream, event);

        assertEquals(
                "{" +
                        "\"index\":{" +
                        "\"_index\":\"awesome-project-production-subproject-1970.01.01\"," +
                        "\"_type\":\"LogEvent\"," +
                        "\"_id\":\"AAAAAAAAAAAAAAAAAAAQAJlPj884PwAA\"" +
                        "}" +
                        "}",
                stream.toString()
        );
    }

    @Test
    public void shouldReplaceSpaceWithUnderscoreInProjectForIndexName() throws IOException {
        final Event event = EventBuilder.create(TimeUtil.UNIX_EPOCH, "00000000-0000-1000-994f-8fcf383f0000")
                .tag("properties", Variant.ofContainer(Container.builder()
                        .tag("project", Variant.ofString("awesome project"))
                        .tag(CommonTags.SUBPROJECT_TAG.getName(), Variant.ofString("subproject"))
                        .tag("environment", Variant.ofString("production"))
                        .build()
                ))
                .build();

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        eventProcess(stream, event);

        assertEquals(
                "{" +
                        "\"index\":{" +
                        "\"_index\":\"awesome_project-production-subproject-1970.01.01\"," +
                        "\"_type\":\"LogEvent\"," +
                        "\"_id\":\"AAAAAAAAAAAAAAAAAAAQAJlPj884PwAA\"" +
                        "}" +
                        "}",
                stream.toString()
        );
    }

    private void eventProcess(ByteArrayOutputStream stream, Event event) throws IOException {
        String index = LogEventIndexResolver.forPolicy(IndexPolicy.DAILY).resolve(event).orElseThrow(NullPointerException::new);
        String eventId = Optional.ofNullable(EventUtil.extractStringId(event)).orElseThrow(NullPointerException::new);
        IndexToElasticJsonWriter.writeIndex(stream, index, eventId);
    }
}
