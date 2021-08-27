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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

/**
 * @author Gregory Koshelev
 */
public class IndexResolverTest {
    private static final List<IndexResolver> TAGS_INDEX_RESOLVER_ONLY;
    private static final List<IndexResolver> STATIC_INDEX_RESOLVER_ONLY;
    private static final List<IndexResolver> SEVERAL_INDEX_RESOLVERS;
    private static final Event PLAIN_EVENT;

    static {
        TAGS_INDEX_RESOLVER_ONLY = prepareTagsIndexResolverOnly();
        STATIC_INDEX_RESOLVER_ONLY = prepareStaticIndexResolverOnly();
        SEVERAL_INDEX_RESOLVERS = prepareSeveralIndexResolvers();
        PLAIN_EVENT = EventBuilder.create(
                TimeUtil.dateTimeToUnixTicks(ZonedDateTime.of(2019, 12, 1, 10, 42, 0, 0, ZoneOffset.UTC)),
                "00000000-0000-0000-0000-000000000000").
                tag("properties", Variant.ofContainer(
                        Container.builder().
                                tag("project", Variant.ofString("proj")).
                                tag("environment", Variant.ofString("dev")).
                                tag("subproject", Variant.ofString("subproj")).
                                build())).
                build();
    }

    @Test
    public void shouldResolveDailyIndex() {
        Optional<String> index = IndexResolver.forPolicy(IndexPolicy.DAILY, TAGS_INDEX_RESOLVER_ONLY).resolve(PLAIN_EVENT);

        Assert.assertTrue(index.isPresent());
        Assert.assertEquals("proj-dev-subproj-2019.12.01", index.get());
    }

    @Test
    public void shouldResolveIlmIndex() {
        Optional<String> index = IndexResolver.forPolicy(IndexPolicy.ILM, TAGS_INDEX_RESOLVER_ONLY).resolve(PLAIN_EVENT);

        Assert.assertTrue(index.isPresent());
        Assert.assertEquals("proj-dev-subproj", index.get());
    }

    @Test
    public void shouldResolveStaticIndex() {
        Optional<String> index = IndexResolver.forPolicy(IndexPolicy.STATIC, STATIC_INDEX_RESOLVER_ONLY).resolve(PLAIN_EVENT);

        Assert.assertTrue(index.isPresent());
        Assert.assertEquals("static-index", index.get());
    }

    @Test
    public void shouldResolveIndexByPrimaryIndexResolver() {
        final Event event = EventBuilder.create(
                TimeUtil.dateTimeToUnixTicks(ZonedDateTime.of(2019, 12, 1, 10, 42, 0, 0, ZoneOffset.UTC)),
                "00000000-0000-0000-0000-000000000000").
                tag("properties", Variant.ofContainer(
                        Container.builder().
                                tag("elk-index", Variant.ofString("custom-index")).
                                tag("project", Variant.ofString("proj")).
                                tag("environment", Variant.ofString("dev")).
                                tag("subproject", Variant.ofString("subproj")).
                                build())).
                build();

        Optional<String> index = IndexResolver.forPolicy(IndexPolicy.ILM, SEVERAL_INDEX_RESOLVERS).resolve(event);

        Assert.assertTrue(index.isPresent());
        Assert.assertEquals("custom-index", index.get());
    }

    @Test
    public void shouldResolveIndexByFirstIndexResolverWithNotEmptyResult() {
        Optional<String> index = IndexResolver.forPolicy(IndexPolicy.ILM, SEVERAL_INDEX_RESOLVERS).resolve(PLAIN_EVENT);

        Assert.assertTrue(index.isPresent());
        Assert.assertEquals("proj-dev-subproj", index.get());
    }

    @Test
    public void shouldSanitizeIndexName() {
        final Event event = EventBuilder.create(
                TimeUtil.dateTimeToUnixTicks(ZonedDateTime.of(2019, 12, 1, 10, 42, 0, 0, ZoneOffset.UTC)),
                "00000000-0000-0000-0000-000000000000").
                tag("properties", Variant.ofContainer(
                        Container.builder().
                                tag("project", Variant.ofString("Project<To,Test>")).
                                tag("environment", Variant.ofString("D.E.V")).
                                tag("subproject", Variant.ofString("> Ю")).
                                build())).
                build();

        Optional<String> index = IndexResolver.forPolicy(IndexPolicy.DAILY, TAGS_INDEX_RESOLVER_ONLY).resolve(event);

        Assert.assertTrue(index.isPresent());
        Assert.assertEquals("project_to_test_-d.e.v-___-2019.12.01", index.get());
    }

    @Test
    public void shouldIgnoreBadIndexName() {
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

        Optional<String> index = IndexResolver.forPolicy(IndexPolicy.DAILY, TAGS_INDEX_RESOLVER_ONLY).resolve(event);

        Assert.assertFalse(index.isPresent());
    }

    private static List<IndexResolver> prepareTagsIndexResolverOnly() {
        List<IndexResolver> indexResolvers = new ArrayList<>();
        Properties props = new Properties();
        props.setProperty("tags", "properties/project,properties/environment?,properties/subproject?");
        indexResolvers.add(new TagsIndexResolver(props));
        return indexResolvers;
    }

    private static List<IndexResolver> prepareStaticIndexResolverOnly() {
        List<IndexResolver> indexResolvers = new ArrayList<>();
        Properties props = new Properties();
        props.setProperty("index.name", "static-index");
        indexResolvers.add(new StaticIndexResolver(props));
        return indexResolvers;
    }

    private static List<IndexResolver> prepareSeveralIndexResolvers() {
        List<IndexResolver> indexResolvers = new ArrayList<>();
        Properties props1 = new Properties();
        props1.setProperty("tags", "properties/elk-index");
        indexResolvers.add(new TagsIndexResolver(props1));
        Properties props2 = new Properties();
        props2.setProperty("tags", "properties/project,properties/environment?,properties/subproject?");
        indexResolvers.add(new TagsIndexResolver(props2));
        return indexResolvers;
    }
}
