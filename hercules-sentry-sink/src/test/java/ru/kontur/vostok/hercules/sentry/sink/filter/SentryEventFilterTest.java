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
public class SentryEventFilterTest {

    private final String SHOULD_FILTER_OUT = " The event should be filtered out";
    private final String SHOULD_PASS = " The event should be passed through the filter";

    @Test
    public void sentryEventFilterTest() {
        Properties properties = new Properties();
        properties.setProperty("patterns", "my_project:prod:*,my_project:testing:my_subproject");
        SentryBlacklistEventFilter blacklist = new SentryBlacklistEventFilter(properties);
        SentryWhitelistEventFilter whitelist = new SentryWhitelistEventFilter(properties);

        Event event = getEventBuilder().build();
        String message = "The event doesn't contain properties tag and doesn't equal to any pattern.";
        Assert.assertTrue(message + SHOULD_PASS, blacklist.test(event));
        Assert.assertFalse(message + SHOULD_FILTER_OUT, whitelist.test(event));

        event = getEventBuilder().
                tag("properties", Variant.ofContainer(Container.of("project", Variant.ofString("my_project")))).
                build();
        message = "The event contain only project tag and doesn't equal to any pattern.";
        Assert.assertTrue(message + SHOULD_PASS, blacklist.test(event));
        Assert.assertFalse(message + SHOULD_FILTER_OUT, whitelist.test(event));

        event = getEventBuilder().
                tag(
                        "properties",
                        Variant.ofContainer(
                                Container.builder().
                                        tag("project", Variant.ofString("my_project")).
                                        tag("environment", Variant.ofString("prod")).
                                        build())).
                build();
        message = "The event doesn't contain subproject tag and equals to pattern 'my_project:prod:*'.";
        Assert.assertFalse(message + SHOULD_FILTER_OUT, blacklist.test(event));
        Assert.assertTrue(message + SHOULD_PASS, whitelist.test(event));

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
        message = "The event contains outsider value of subproject tag and equals to pattern 'my_project:prod:*'.";
        Assert.assertFalse(message + SHOULD_FILTER_OUT, blacklist.test(event));
        Assert.assertTrue(message + SHOULD_PASS, whitelist.test(event));

        event = getEventBuilder().
                tag(
                        "properties",
                        Variant.ofContainer(
                                Container.builder().
                                        tag("project", Variant.ofString("my_project")).
                                        tag("environment", Variant.ofString("testing")).
                                        build())).
                build();
        message = "The event doesn't contain subproject tag and and doesn't equal to any pattern.";
        Assert.assertTrue(message + SHOULD_PASS, blacklist.test(event));
        Assert.assertFalse(message + SHOULD_FILTER_OUT, whitelist.test(event));

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
        message = "The event equals to pattern 'my_project:testing:my_subproject'.";
        Assert.assertFalse(message + SHOULD_FILTER_OUT, blacklist.test(event));
        Assert.assertTrue(message + SHOULD_PASS, whitelist.test(event));

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
        message = "The event contains outsider value of subproject tag and doesn't equal to any pattern.";
        Assert.assertTrue(message + SHOULD_PASS, blacklist.test(event));
        Assert.assertFalse(message + SHOULD_FILTER_OUT, whitelist.test(event));

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
        message = "The event contains outsider value of project tag and doesn't equal to any pattern.";
        Assert.assertTrue(message + SHOULD_PASS, blacklist.test(event));
        Assert.assertFalse(message + SHOULD_FILTER_OUT, whitelist.test(event));

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
        message = "The event contains outsider values of project and subproject tag and doesn't equal to any pattern.";
        Assert.assertTrue(message + SHOULD_PASS, blacklist.test(event));
        Assert.assertFalse(message + SHOULD_FILTER_OUT, whitelist.test(event));
    }

    @Test
    public void valueSanitationTest() {
        Properties properties = new Properties();
        properties.setProperty("patterns", "MY_Project:testing:multi_word_subproject_value");
        SentryBlacklistEventFilter blacklist = new SentryBlacklistEventFilter(properties);
        SentryWhitelistEventFilter whitelist = new SentryWhitelistEventFilter(properties);

        Event event = getEventBuilder().
                tag(
                        "properties",
                        Variant.ofContainer(
                                Container.builder().
                                        tag("project", Variant.ofString("my_project")).
                                        tag("environment", Variant.ofString("TESTING")).
                                        tag("subproject", Variant.ofString("multi.word subproject/value")).
                                        build())).
                build();
        String message = "The event equals to pattern considering sanitation.";
        Assert.assertFalse(message + SHOULD_FILTER_OUT, blacklist.test(event));
        Assert.assertTrue(message + SHOULD_PASS, whitelist.test(event));
    }

    @Test
    public void emptyPatternTest() {
        Properties properties = new Properties();
        properties.setProperty("patterns", "my_project:testing:,my_project::*");
        SentryBlacklistEventFilter blacklist = new SentryBlacklistEventFilter(properties);
        SentryWhitelistEventFilter whitelist = new SentryWhitelistEventFilter(properties);

        Event event = getEventBuilder().
                tag(
                        "properties",
                        Variant.ofContainer(
                                Container.builder().
                                        tag("project", Variant.ofString("my_project")).
                                        tag("environment", Variant.ofString("testing")).
                                        tag("subproject", Variant.ofString("")).
                                        build())).
                build();
        String message = "The event contain empty value of subproject tag and equals to pattern 'my_project:testing:'.";
        Assert.assertFalse(message + SHOULD_FILTER_OUT, blacklist.test(event));
        Assert.assertTrue(message + SHOULD_PASS, whitelist.test(event));

        event = getEventBuilder().
                tag(
                        "properties",
                        Variant.ofContainer(
                                Container.builder().
                                        tag("project", Variant.ofString("my_project")).
                                        tag("environment", Variant.ofString("testing")).
                                        build())).
                build();
        message = "The event doesn't contain subproject tag and equals to pattern 'my_project:testing:'.";
        Assert.assertFalse(message + SHOULD_FILTER_OUT, blacklist.test(event));
        Assert.assertTrue(message + SHOULD_PASS, whitelist.test(event));

        event = getEventBuilder().
                tag(
                        "properties",
                        Variant.ofContainer(
                                Container.builder().
                                        tag("project", Variant.ofString("my_project")).
                                        tag("subproject", Variant.ofString("other_subproject")).
                                        build())).
                build();
        message = "The event doesn't contain environment tag, contains outsider value of subproject tag and equals to pattern 'my_project::*'.";
        Assert.assertFalse(message + SHOULD_FILTER_OUT, blacklist.test(event));
        Assert.assertTrue(message + SHOULD_PASS, whitelist.test(event));

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
        message = "The event contains outsider value of subproject tag and doesn't equal to any pattern.";
        Assert.assertTrue(message + SHOULD_PASS, blacklist.test(event));
        Assert.assertFalse(message + SHOULD_FILTER_OUT, whitelist.test(event));
    }

    private EventBuilder getEventBuilder() {
        return EventBuilder.create(
                TimeUtil.millisToTicks(System.currentTimeMillis()),
                "00000000-0000-0000-0000-000000000000");
    }
}
