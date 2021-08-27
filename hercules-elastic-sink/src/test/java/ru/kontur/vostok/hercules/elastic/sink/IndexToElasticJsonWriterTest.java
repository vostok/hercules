package ru.kontur.vostok.hercules.elastic.sink;

import org.junit.Test;
import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.EventBuilder;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.protocol.util.EventUtil;
import ru.kontur.vostok.hercules.util.time.TimeUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Optional;

import static org.junit.Assert.assertEquals;

public class IndexToElasticJsonWriterTest {

    @Test
    public void shouldWriteIndex() throws Exception {
        final Event event = EventBuilder.create(
                TimeUtil.dateTimeToUnixTicks(ZonedDateTime.of(2019, 12, 1, 10, 42, 0, 0, ZoneOffset.UTC)),
                "00000000-0000-0000-0000-000000000000").
                tag("properties", Variant.ofContainer(
                        Container.builder().
                                tag("elk-index", Variant.ofString("just-some-index-value")).
                                build())).
                build();

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        eventProcess(stream, event);
        assertEquals(
                "{" +
                        "\"index\":{" +
                        "\"_index\":\"just-some-index-value-2019.12.01\"," +
                        "\"_type\":\"_doc\"," +
                        "\"_id\":\"ADf2VSCtzAAAAAAAAAAAAAAAAAAAAAAA\"" +
                        "}" +
                        "}",
                stream.toString()
        );
    }

    private void eventProcess(ByteArrayOutputStream stream, Event event) throws IOException {
        String index = "just-some-index-value-2019.12.01";
        String eventId = Optional.ofNullable(EventUtil.extractStringId(event)).orElseThrow(NullPointerException::new);
        IndexToElasticJsonWriter.writeIndex(stream, index, eventId);
    }
}
