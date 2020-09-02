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

import static org.junit.Assert.assertFalse;

/**
 * @author Gregory Koshelev
 */
public class IndexResolverTest {
    public static final Properties INDEX_RESOLVER_PROPERTIES;
    static {
        Properties props = new Properties();
        props.setProperty(IndexResolver.Props.INDEX_PATH.name(), "properties/elk-index");
        props.setProperty(IndexResolver.Props.INDEX_TAGS.name(), "properties/project,properties/environment?,properties/subproject?");
        INDEX_RESOLVER_PROPERTIES = props;
    }

    @Test
    public void shouldNotResolveIndexIfNoSuitableTags() {
        IndexResolver indexResolver = IndexResolver.forPolicy(IndexPolicy.DAILY, INDEX_RESOLVER_PROPERTIES);
        final Event event = EventBuilder.create(0, "00000000-0000-1000-994f-8fcf383f0000") //TODO: fix me!
                .build();
        Optional<String> result = indexResolver.resolve(event);
        assertFalse(result.isPresent());
    }

    @Test
    public void shouldResolveDailyIndex() {
        IndexResolver indexResolver = IndexResolver.forPolicy(IndexPolicy.DAILY, INDEX_RESOLVER_PROPERTIES);
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
        Assert.assertEquals("proj-dev-subproj-2019.12.01", index.get());
    }

    @Test
    public void shouldResolveIlmIndex() {
        IndexResolver indexResolver = IndexResolver.forPolicy(IndexPolicy.ILM, INDEX_RESOLVER_PROPERTIES);
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
    public void shouldIgnoreBadIndexName() {
        IndexResolver indexResolver = IndexResolver.forPolicy(IndexPolicy.DAILY, INDEX_RESOLVER_PROPERTIES);
        final Event event = EventBuilder.create(
                TimeUtil.dateTimeToUnixTicks(ZonedDateTime.of(2019, 12, 1, 10, 42, 0, 0, ZoneOffset.UTC)),
                "00000000-0000-0000-0000-000000000000").
                tag("properties", Variant.ofContainer(
                        Container.builder().
                                tag("project", Variant.ofString("{#project}")).
                                tag("environment", Variant.ofString("dev")).
                                tag("subproject", Variant.ofString("subproj")).
                                build())).
                build();

        Optional<String> index = indexResolver.resolve(event);
        Assert.assertFalse(index.isPresent());
    }


    @Test
    public void shouldSanitizeIndexName() {
        IndexResolver indexResolver = IndexResolver.forPolicy(IndexPolicy.DAILY, INDEX_RESOLVER_PROPERTIES);
        final Event event = EventBuilder.create(
                TimeUtil.dateTimeToUnixTicks(ZonedDateTime.of(2019, 12, 1, 10, 42, 0, 0, ZoneOffset.UTC)),
                "00000000-0000-0000-0000-000000000000").
                tag("properties", Variant.ofContainer(
                        Container.builder().
                                tag("project", Variant.ofString("Project<To,Test>")).
                                tag("environment", Variant.ofString("D.E.V")).
                                tag("subproject", Variant.ofString(">Ю")).
                                build())).
                build();

        Optional<String> index = indexResolver.resolve(event);
        Assert.assertTrue(index.isPresent());
        Assert.assertEquals("project_to_test_-d.e.v-__-2019.12.01", index.get());
    }

    @Test
    public void shouldResolveIndexNameWithoutOptionalParts() {
        IndexResolver indexResolver = IndexResolver.forPolicy(IndexPolicy.ILM, INDEX_RESOLVER_PROPERTIES);
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

    @Test
    public void shouldResolvePredefinedIndexName() {
        IndexResolver indexResolver = IndexResolver.forPolicy(IndexPolicy.ILM, INDEX_RESOLVER_PROPERTIES);
        final Event event = EventBuilder.create(
                TimeUtil.dateTimeToUnixTicks(ZonedDateTime.of(2019, 12, 1, 10, 42, 0, 0, ZoneOffset.UTC)),
                "00000000-0000-0000-0000-000000000000").
                tag("properties", Variant.ofContainer(
                        Container.builder().
                                tag("project", Variant.ofString("proj")).
                                tag("environment", Variant.ofString("dev")).
                                tag("subproject", Variant.ofString("subproj")).
                                tag("elk-index", Variant.ofString("custom-index")).
                                build())).
                build();

        Optional<String> index = indexResolver.resolve(event);
        Assert.assertTrue(index.isPresent());
        Assert.assertEquals("custom-index", index.get());
    }
}
