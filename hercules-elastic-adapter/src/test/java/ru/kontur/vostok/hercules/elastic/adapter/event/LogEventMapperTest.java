package ru.kontur.vostok.hercules.elastic.adapter.event;

import org.junit.Test;
import ru.kontur.vostok.hercules.elastic.adapter.document.DocumentReader;
import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.EventBuilder;
import ru.kontur.vostok.hercules.protocol.HerculesProtocolAssert;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.protocol.Vector;
import ru.kontur.vostok.hercules.tags.CommonTags;
import ru.kontur.vostok.hercules.tags.ElasticSearchTags;
import ru.kontur.vostok.hercules.tags.LogEventTags;
import ru.kontur.vostok.hercules.util.functional.Result;
import ru.kontur.vostok.hercules.util.time.TimeUtil;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.assertTrue;

/**
 * @author Gregory Koshelev
 */
public class LogEventMapperTest {
    @Test
    public void shouldCreateComplexEvent() {
        byte[] logEvent = ("{\"@timestamp\": \"1970-01-01T01:01:01.000Z\", \"level\": \"INFO\", \"message\": \"Test message\", " +
                "\"map\": {\"field1\": \"value1\", \"field2\": \"value2\"}, " +
                "\"listOfIntegers\": [1, 2, 3], " +
                "\"listOfStrings\": [\"a\", \"b\", \"c\"]}").getBytes(StandardCharsets.UTF_8);
        Map<String, Object> document = DocumentReader.read(logEvent);
        String index = "test_index";

        Result<Event, String> result =
                LogEventMapper.from(
                        document,
                        Collections.singletonMap(CommonTags.PROJECT_TAG.getName(), Variant.ofString("test_project")),
                        index);
        assertTrue(result.isOk());

        Event actual = result.get();
        Event expected = EventBuilder.create(TimeUtil.secondsToTicks(1 * 60 * 60 + 1 * 60 + 1), actual.getUuid()).
                tag(LogEventTags.UTC_OFFSET_TAG.getName(), Variant.ofLong(0)).
                tag(LogEventTags.LEVEL_TAG.getName(), Variant.ofString("INFO")).
                tag(LogEventTags.MESSAGE_TAG.getName(), Variant.ofString("Test message")).
                tag(CommonTags.PROPERTIES_TAG.getName(),
                        Variant.ofContainer(Container.builder().
                                tag("@timestamp", Variant.ofString("1970-01-01T01:01:01.000Z")).
                                tag("map", Variant.ofContainer(Container.builder().
                                        tag("field1", Variant.ofString("value1")).
                                        tag("field2", Variant.ofString("value2")).
                                        build())).
                                tag("listOfIntegers", Variant.ofVector(Vector.ofIntegers(1, 2, 3))).
                                tag("listOfStrings", Variant.ofVector(Vector.ofStrings("a", "b", "c"))).
                                tag(CommonTags.PROJECT_TAG.getName(), Variant.ofString("test_project")).
                                tag(ElasticSearchTags.ELK_INDEX_TAG.getName(), Variant.ofString(index)).
                                build())).
                build();

        HerculesProtocolAssert.assertEquals(expected, actual);
    }
}
