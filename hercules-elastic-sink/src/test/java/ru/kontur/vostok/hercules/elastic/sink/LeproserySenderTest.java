package ru.kontur.vostok.hercules.elastic.sink;

import org.junit.Assert;
import org.junit.Test;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.json.mapping.EventMappingWriter;
import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.TinyString;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.protocol.util.ContainerUtil;
import ru.kontur.vostok.hercules.protocol.EventBuilder;
import ru.kontur.vostok.hercules.tags.CommonTags;
import ru.kontur.vostok.hercules.tags.LogEventTags;
import ru.kontur.vostok.hercules.util.time.TimeUtil;
import ru.kontur.vostok.hercules.uuid.UuidGenerator;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.Properties;

import static org.mockito.Mockito.mock;

/**
 * @author Petr Demenev
 */
public class LeproserySenderTest {

    private static MetricsCollector metricsCollectorMock = mock(MetricsCollector.class);

    @Test
    public void toLeproseryEventTest() {
        Event originalEvent = createEvent();

        EventMappingWriter eventMappingWriter = createEventMappingWriter();

        Properties properties = new Properties();
        properties.setProperty("stream", "leprosery-stream");
        properties.setProperty("apiKey", "123");
        properties.setProperty("gate.client.urls", "localhost:8080");
        LeproserySender leproserySender = new LeproserySender(properties, metricsCollectorMock, eventMappingWriter);

        Event leproseryEvent = leproserySender.toLeproseryEvent(
                originalEvent,
                "my-original-index",
                "my error reason").get();

        Assert.assertEquals("my error reason",
                ContainerUtil.extract(leproseryEvent.getPayload(), LogEventTags.MESSAGE_TAG).get());
        Assert.assertEquals("hercules", getValueFromProperties(leproseryEvent, "project"));
        Assert.assertEquals("hercules-elastic-sink", getValueFromProperties(leproseryEvent, "service"));
        Assert.assertEquals("my-original-index", getValueFromProperties(leproseryEvent, "original-index"));
        Assert.assertEquals("leprosery", getValueFromProperties(leproseryEvent, "elk-index"));
        Assert.assertEquals("{\"@timestamp\":\"2019-10-25T08:55:21.839000000Z\"," +
                        "\"message\":\"Test event\",\"level\":\"info\"," +
                        "\"project\":\"my-project\",\"my_tag\":\"My value\"}",
                getValueFromProperties(leproseryEvent, "text"));
    }

    private Event createEvent() {
        return EventBuilder.create()
                .version(1)
                .timestamp(TimeUtil.dateTimeToUnixTicks(ZonedDateTime.parse("2019-10-25T08:55:21.839000000Z")))
                .uuid(UuidGenerator.getClientInstance().next())
                .tag(LogEventTags.MESSAGE_TAG.getName(), Variant.ofString("Test event"))
                .tag(LogEventTags.LEVEL_TAG.getName(), Variant.ofString("info"))
                .tag(CommonTags.PROPERTIES_TAG.getName(), Variant.ofContainer(Container.builder()
                        .tag(CommonTags.PROJECT_TAG.getName(), Variant.ofString("my-project"))
                        .tag("my_tag", Variant.ofString("My value"))
                        .build()))
                .build();
    }

    private EventMappingWriter createEventMappingWriter() {
        Properties properties = new Properties();
        properties.setProperty("file", "resource://log-event.mapping");
        return new EventMappingWriter(properties);
    }

    private String getValueFromProperties(Event event, String tag) {
        final Optional<Container> propertiesContainer = ContainerUtil.
                extract(event.getPayload(), CommonTags.PROPERTIES_TAG);
        return new String((byte[]) propertiesContainer.get().get(TinyString.of(tag)).getValue());//FIXME: Refactoring is needed
    }
}
