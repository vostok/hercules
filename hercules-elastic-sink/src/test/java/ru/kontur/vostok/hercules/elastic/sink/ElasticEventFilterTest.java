package ru.kontur.vostok.hercules.elastic.sink;

import org.junit.Assert;
import org.junit.Test;
import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.EventBuilder;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.util.time.TimeUtil;

import java.util.Properties;

/**
 * @author Gregory Koshelev
 */
public class ElasticEventFilterTest {
    @Test
    public void shouldPassCorrectEvents() {
        ElasticEventFilter filter = new ElasticEventFilter(new Properties());

        Event event = getEventBuilder().
                tag("properties", Variant.ofContainer(Container.of("elk-index", Variant.ofString("my_project-my_subproject")))).
                build();
        Assert.assertTrue(filter.test(event));

        event = getEventBuilder().
                tag("properties", Variant.ofContainer(Container.of("project", Variant.ofString("my_project")))).
                build();
        Assert.assertTrue(filter.test(event));

        event = getEventBuilder().
                tag(
                        "properties",
                        Variant.ofContainer(
                                Container.builder().
                                        tag("project", Variant.ofString("my_project")).
                                        tag("environment", Variant.ofString("testing")).
                                        build())).
                build();
        Assert.assertTrue(filter.test(event));

        event = getEventBuilder().
                tag(
                        "properties",
                        Variant.ofContainer(
                                Container.builder().
                                tag("project", Variant.ofString("my_project")).
                                tag("subproject", Variant.ofString("my_subproject")).
                                build())).
                build();
        Assert.assertTrue(filter.test(event));

        event = getEventBuilder().
                tag(
                        "properties",
                        Variant.ofContainer(
                                Container.builder().
                                tag("project", Variant.ofString("my_project")).
                                tag("environment", Variant.ofString("testing")).
                                tag("subproject", Variant.ofString("my_subproject")).
                                build())).
                build();
        Assert.assertTrue(filter.test(event));
    }

    @Test
    public void shouldDenyInvalidEvents() {
        ElasticEventFilter filter = new ElasticEventFilter(new Properties());

        Event event = getEventBuilder().build();
        Assert.assertFalse(filter.test(event));

        event = getEventBuilder().
                tag("properties", Variant.ofContainer(Container.empty())).
                build();
        Assert.assertFalse(filter.test(event));

        event = getEventBuilder().
                tag("properties", Variant.ofContainer(Container.of("elk-index", Variant.ofString("$$$")))).
                build();
        Assert.assertFalse(filter.test(event));

        event = getEventBuilder().
                tag("properties", Variant.ofContainer(Container.of("project", Variant.ofString("$$$")))).
                build();
        Assert.assertFalse(filter.test(event));

        event = getEventBuilder().
                tag(
                        "properties",
                        Variant.ofContainer(
                                Container.builder().
                                        tag("project", Variant.ofString("my_project")).
                                        tag("environment", Variant.ofString("$$$")).
                                        build())).
                build();
        Assert.assertFalse(filter.test(event));

        event = getEventBuilder().
                tag(
                        "properties",
                        Variant.ofContainer(
                                Container.builder().
                                        tag("project", Variant.ofString("my_project")).
                                        tag("subproject", Variant.ofString("$$$")).
                                        build())).
                build();
        Assert.assertFalse(filter.test(event));

        event = getEventBuilder().
                tag(
                        "properties",
                        Variant.ofContainer(
                                Container.builder().
                                        tag("project", Variant.ofString("my_project")).
                                        tag("environment", Variant.ofString("testing")).
                                        tag("subproject", Variant.ofString("$$$")).
                                        build())).
                build();
        Assert.assertFalse(filter.test(event));
    }

    private EventBuilder getEventBuilder() {
        return EventBuilder.create(
                TimeUtil.millisToTicks(System.currentTimeMillis()),
                "00000000-0000-0000-0000-000000000000");
    }
}
