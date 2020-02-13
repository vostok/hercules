package ru.kontur.vostok.hercules.sentry.sink.filter;

import org.junit.Assert;
import org.junit.Test;
import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.EventBuilder;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.util.time.TimeUtil;

import java.util.Properties;

/**
 * @author Petr Demenev
 */
public class SentryWhitelistEventFilterTest {

    @Test
    public void filterTest() {
        Properties properties = new Properties();
        properties.setProperty("patterns", "my_project:prod:*,my_project:testing:my_subproject");
        SentryWhitelistEventFilter filter = new SentryWhitelistEventFilter(properties);

        Event event = getEventBuilder().build();
        Assert.assertFalse(filter.test(event));

        event = getEventBuilder().
                tag("properties", Variant.ofContainer(Container.of("project", Variant.ofString("my_project")))).
                build();
        Assert.assertFalse(filter.test(event));

        event = getEventBuilder().
                tag(
                        "properties",
                        Variant.ofContainer(
                                Container.builder().
                                        tag("project", Variant.ofString("my_project")).
                                        tag("environment", Variant.ofString("prod")).
                                        build())).
                build();
        Assert.assertTrue(filter.test(event));

        event = getEventBuilder().
                tag(
                        "properties",
                        Variant.ofContainer(
                                Container.builder().
                                        tag("project", Variant.ofString("my_project")).
                                        tag("environment", Variant.ofString("prod")).
                                        tag("subproject", Variant.ofString("other_subproject")).
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
                                        tag("subproject", Variant.ofString("other_subproject")).
                                        build())).
                build();
        Assert.assertFalse(filter.test(event));

        event = getEventBuilder().
                tag(
                        "properties",
                        Variant.ofContainer(
                                Container.builder().
                                        tag("project", Variant.ofString("other_project")).
                                        tag("environment", Variant.ofString("testing")).
                                        tag("subproject", Variant.ofString("my_subproject")).
                                        build())).
                build();
        Assert.assertFalse(filter.test(event));

        event = getEventBuilder().
                tag(
                        "properties",
                        Variant.ofContainer(
                                Container.builder().
                                        tag("project", Variant.ofString("other_project")).
                                        tag("environment", Variant.ofString("prod")).
                                        tag("subproject", Variant.ofString("other_subproject")).
                                        build())).
                build();
        Assert.assertFalse(filter.test(event));
    }

    @Test
    public void ignoreCaseTest() {
        Properties properties = new Properties();
        properties.setProperty("patterns", "my_Project:testing:my_subproject");
        SentryWhitelistEventFilter filter = new SentryWhitelistEventFilter(properties);

        Event event = getEventBuilder().
                tag(
                        "properties",
                        Variant.ofContainer(
                                Container.builder().
                                        tag("project", Variant.ofString("my_project")).
                                        tag("environment", Variant.ofString("Testing")).
                                        tag("subproject", Variant.ofString("MY_SUBPROJECT")).
                                        build())).
                build();
        Assert.assertTrue(filter.test(event));
    }

    private EventBuilder getEventBuilder() {
        return EventBuilder.create(
                TimeUtil.millisToTicks(System.currentTimeMillis()),
                "00000000-0000-0000-0000-000000000000");
    }
}
