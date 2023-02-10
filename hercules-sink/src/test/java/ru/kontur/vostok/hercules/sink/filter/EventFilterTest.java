package ru.kontur.vostok.hercules.sink.filter;

import org.junit.jupiter.api.Test;
import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.EventBuilder;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.util.time.TimeUtil;

import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Gregory Koshelev
 */
public class EventFilterTest {
    @Test
    public void shouldInitializeListOfFiltersFromProperties() {
        Properties properties = new Properties();
        properties.setProperty("0.class", "ru.kontur.vostok.hercules.sink.filter.SwitcherEventFilter");
        properties.setProperty("1.class", "ru.kontur.vostok.hercules.sink.filter.SwitcherEventFilter");
        properties.setProperty("0.props.on", "false");
        properties.setProperty("1.props.on", "true");

        List<EventFilter> filters = EventFilter.from(properties);
        assertEquals(2, filters.size());
        assertEquals(SwitcherEventFilter.class, filters.get(0).getClass());
        assertEquals(SwitcherEventFilter.class, filters.get(1).getClass());

        Event event = getEventBuilder().build();
        SwitcherEventFilter switcher = (SwitcherEventFilter) filters.get(0);
        /* Switcher '0' is off as property 'on' is false */
        assertFalse(switcher.test(event));

        switcher = (SwitcherEventFilter) filters.get(1);
        /* Switcher '1' is on as property 'on is true */
        assertTrue(switcher.test(event));
    }

    @Test
    public void blacklistTest() {
        Properties properties = new Properties();
        properties.setProperty("types", "STRING,STRING,INTEGER");
        properties.setProperty("paths", "properties/project,properties/environment,properties/id");
        properties.setProperty("patterns", "my_project:staging:123,test_project:production:456,bad_project:*:*");

        BlacklistEventFilter filter = new BlacklistEventFilter(properties);

        Event event = getEventBuilder().build();
        assertTrue(filter.test(event));

        event = getEventBuilder().
                tag("properties", Variant.ofContainer(Container.of("project", Variant.ofString("my_project")))).
                build();
        assertTrue(filter.test(event));

        event = getEventBuilder().
                tag("properties", Variant.ofContainer(Container.of("project", Variant.ofString("bad_project")))).
                build();
        assertFalse(filter.test(event));

        event = getEventBuilder().
                tag(
                        "properties",
                        Variant.ofContainer(
                                Container.builder().
                                        tag("project", Variant.ofString("bad_project")).
                                        tag("environment", Variant.ofString("testing")).
                                        tag("id", Variant.ofInteger(123)).
                                        build())).
                build();
        assertFalse(filter.test(event));

        event = getEventBuilder().
                tag(
                        "properties",
                        Variant.ofContainer(
                                Container.builder().
                                        tag("project", Variant.ofString("test_project")).
                                        tag("environment", Variant.ofString("production")).
                                        tag("id", Variant.ofInteger(456)).
                                        build())).
                build();
        assertFalse(filter.test(event));

        event = getEventBuilder().
                tag(
                        "properties",
                        Variant.ofContainer(
                                Container.builder().
                                        tag("project", Variant.ofString("test_project")).
                                        tag("environment", Variant.ofString("staging")).
                                        tag("id", Variant.ofInteger(456)).
                                        build())).
                build();
        assertTrue(filter.test(event));
    }

    @Test
    public void whitelistTest() {
        Properties properties = new Properties();
        properties.setProperty("types", "STRING,STRING,INTEGER");
        properties.setProperty("paths", "properties/project,properties/environment,properties/id");
        properties.setProperty("patterns", "my_project:staging:123,test_project:production:456,bad_project:*:*");

        WhitelistEventFilter filter = new WhitelistEventFilter(properties);

        Event event = getEventBuilder().build();
        assertFalse(filter.test(event));

        event = getEventBuilder().
                tag("properties", Variant.ofContainer(Container.of("project", Variant.ofString("my_project")))).
                build();
        assertFalse(filter.test(event));

        event = getEventBuilder().
                tag("properties", Variant.ofContainer(Container.of("project", Variant.ofString("bad_project")))).
                build();
        assertTrue(filter.test(event));

        event = getEventBuilder().
                tag(
                        "properties",
                        Variant.ofContainer(
                                Container.builder().
                                        tag("project", Variant.ofString("bad_project")).
                                        tag("environment", Variant.ofString("testing")).
                                        tag("id", Variant.ofInteger(123)).
                                        build())).
                build();
        assertTrue(filter.test(event));

        event = getEventBuilder().
                tag(
                        "properties",
                        Variant.ofContainer(
                                Container.builder().
                                        tag("project", Variant.ofString("test_project")).
                                        tag("environment", Variant.ofString("production")).
                                        tag("id", Variant.ofInteger(456)).
                                        build())).
                build();
        assertTrue(filter.test(event));

        event = getEventBuilder().
                tag(
                        "properties",
                        Variant.ofContainer(
                                Container.builder().
                                        tag("project", Variant.ofString("test_project")).
                                        tag("environment", Variant.ofString("staging")).
                                        tag("id", Variant.ofInteger(456)).
                                        build())).
                build();
        assertFalse(filter.test(event));
    }

    private EventBuilder getEventBuilder() {
        return EventBuilder.create(
                TimeUtil.millisToTicks(System.currentTimeMillis()),
                "00000000-0000-0000-0000-000000000000");
    }
}
