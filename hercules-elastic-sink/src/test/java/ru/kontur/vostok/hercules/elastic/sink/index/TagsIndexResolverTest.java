package ru.kontur.vostok.hercules.elastic.sink.index;

import org.junit.Assert;
import org.junit.Test;
import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.EventBuilder;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.util.time.TimeUtil;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.Properties;

/**
 * @author Petr Demenev
 */
public class TagsIndexResolverTest {
    private static final Properties PROPERTIES;

    static {
        Properties props = new Properties();
        props.setProperty("tags", "properties/project,properties/environment?,properties/subproject?");
        PROPERTIES = props;
    }

    @Test
    public void shouldResolveIndex() {
        TagsIndexResolver indexResolver = new TagsIndexResolver(PROPERTIES);
        final Event event = EventBuilder.create(
                TimeUtil.dateTimeToUnixTicks(ZonedDateTime.of(2019, 12, 1, 10, 42, 0, 0, ZoneOffset.UTC)),
                "00000000-0000-0000-0000-000000000000").
                tag("properties", Variant.ofContainer(
                        Container.builder().
                                tag("project", Variant.ofString("proj")).
                                tag("environment", Variant.ofString("dev")).
                                tag("subproject", Variant.ofString("subproj")).
                                build())).
                build();

        Optional<String> index = indexResolver.resolve(event);
        Assert.assertTrue(index.isPresent());
        Assert.assertEquals("proj-dev-subproj", index.get());
    }

    @Test
    public void shouldNotResolveIndexIfNoSuitableTags() {
        TagsIndexResolver indexResolver = new TagsIndexResolver(PROPERTIES);
        final Event event = EventBuilder.create(
                TimeUtil.dateTimeToUnixTicks(ZonedDateTime.of(2019, 12, 1, 10, 42, 0, 0, ZoneOffset.UTC)),
                "00000000-0000-0000-0000-000000000000").
                build();
        Optional<String> result = indexResolver.resolve(event);
        Assert.assertFalse(result.isPresent());
    }

    @Test
    public void shouldResolveIndexNameWithoutOptionalParts() {
        TagsIndexResolver indexResolver = new TagsIndexResolver(PROPERTIES);
        final Event event = EventBuilder.create(
                TimeUtil.dateTimeToUnixTicks(ZonedDateTime.of(2019, 12, 1, 10, 42, 0, 0, ZoneOffset.UTC)),
                "00000000-0000-0000-0000-000000000000").
                tag("properties", Variant.ofContainer(
                        Container.builder().
                                tag("project", Variant.ofString("proj")).
                                tag("env", Variant.ofString("dev")).// But configured tag is environment
                                tag("subproject", Variant.ofString("subproj")).
                                build())).
                build();

        Optional<String> index = indexResolver.resolve(event);
        Assert.assertTrue(index.isPresent());
        Assert.assertEquals("proj-subproj", index.get());
    }
}
