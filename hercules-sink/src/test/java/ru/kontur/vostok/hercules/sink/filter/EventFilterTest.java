package ru.kontur.vostok.hercules.sink.filter;

import org.junit.Assert;
import org.junit.Test;
import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.EventBuilder;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.util.time.TimeUtil;

import java.util.List;
import java.util.Properties;

/**
 * @author Gregory Koshelev
 */
public class EventFilterTest {
    @Test
    public void shouldInitializeListOfFiltersFromProperties() {
        Properties properties = new Properties();
        properties.setProperty("list", "ru.kontur.vostok.hercules.sink.filter.SwitcherEventFilter,ru.kontur.vostok.hercules.sink.filter.SwitcherEventFilter");
        properties.setProperty("0.on", "false");
        properties.setProperty("1.on", "true");

        List<EventFilter> filters = EventFilter.from(properties);
        Assert.assertEquals(2, filters.size());
        Assert.assertEquals(SwitcherEventFilter.class, filters.get(0).getClass());
        Assert.assertEquals(SwitcherEventFilter.class, filters.get(1).getClass());

        Event event = getEventBuilder().build();

        SwitcherEventFilter switcher = (SwitcherEventFilter) filters.get(0);
        /* Switcher '0' is off as property 'on' is false */
        Assert.assertFalse(switcher.test(event));

        switcher = (SwitcherEventFilter) filters.get(1);
        /* Switcher '1' is on as property 'on is true */
        Assert.assertTrue(switcher.test(event));
    }

    @Test
    public void blacklistTest() {
        Properties properties = new Properties();
        properties.setProperty("paths", "properties/project,properties/environment");
        properties.setProperty("patterns", "my_project:staging,test_project:production,bad_project:*");

        BlacklistEventFilter filter = new BlacklistEventFilter(properties);

        Event event = getEventBuilder().build();
        Assert.assertTrue(filter.test(event));

        event = getEventBuilder().
                tag("properties", Variant.ofContainer(Container.of("project", Variant.ofString("my_project")))).
                build();
        Assert.assertTrue(filter.test(event));

        event = getEventBuilder().
                tag("properties", Variant.ofContainer(Container.of("project", Variant.ofString("bad_project")))).
                build();
        Assert.assertFalse(filter.test(event));

        event = getEventBuilder().
                tag(
                        "properties",
                        Variant.ofContainer(
                                Container.builder().
                                        tag("project", Variant.ofString("bad_project")).
                                        tag("environment", Variant.ofString("testing")).
                                        build())).
                build();
        Assert.assertFalse(filter.test(event));

        event = getEventBuilder().
                tag(
                        "properties",
                        Variant.ofContainer(
                                Container.builder().
                                        tag("project", Variant.ofString("test_project")).
                                        tag("environment", Variant.ofString("production")).
                                        build())).
                build();
        Assert.assertFalse(filter.test(event));

        event = getEventBuilder().
                tag(
                        "properties",
                        Variant.ofContainer(
                                Container.builder().
                                        tag("project", Variant.ofString("test_project")).
                                        tag("environment", Variant.ofString("staging")).
                                        build())).
                build();
        Assert.assertTrue(filter.test(event));
    }

    @Test
    public void whitelistTest() {
        Properties properties = new Properties();
        properties.setProperty("paths", "properties/project,properties/environment");
        properties.setProperty("patterns", "my_project:staging,test_project:production,bad_project:*");

        WhitelistEventFilter filter = new WhitelistEventFilter(properties);

        Event event = getEventBuilder().build();
        Assert.assertFalse(filter.test(event));

        event = getEventBuilder().
                tag("properties", Variant.ofContainer(Container.of("project", Variant.ofString("my_project")))).
                build();
        Assert.assertFalse(filter.test(event));

        event = getEventBuilder().
                tag("properties", Variant.ofContainer(Container.of("project", Variant.ofString("bad_project")))).
                build();
        Assert.assertTrue(filter.test(event));

        event = getEventBuilder().
                tag(
                        "properties",
                        Variant.ofContainer(
                                Container.builder().
                                        tag("project", Variant.ofString("bad_project")).
                                        tag("environment", Variant.ofString("testing")).
                                        build())).
                build();
        Assert.assertTrue(filter.test(event));

        event = getEventBuilder().
                tag(
                        "properties",
                        Variant.ofContainer(
                                Container.builder().
                                        tag("project", Variant.ofString("test_project")).
                                        tag("environment", Variant.ofString("production")).
                                        build())).
                build();
        Assert.assertTrue(filter.test(event));

        event = getEventBuilder().
                tag(
                        "properties",
                        Variant.ofContainer(
                                Container.builder().
                                        tag("project", Variant.ofString("test_project")).
                                        tag("environment", Variant.ofString("staging")).
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
