package ru.kontur.vostok.hercules.graphite.sink.filter;

import org.junit.Assert;
import org.junit.Test;
import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.EventBuilder;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.protocol.Vector;
import ru.kontur.vostok.hercules.tags.MetricsTags;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * @author Aleksandr Yuferov
 */
public class TaggedMetricFilterTest {
    @Test
    public void filterInBlackListModeShouldDenyAllListedValues() {
        TaggedMetricFilter filter = createFilter(false);
        Event event = TaggedEventBuilder.withSingleTag("app", "dagon");

        boolean result = filter.test(event);

        Assert.assertFalse(result);
    }

    @Test
    public void filterInBlackListModeShouldPassAllValuesThatNotMatchesList() {
        TaggedMetricFilter filter = createFilter(false);
        Event event = TaggedEventBuilder.withSingleTag("app", "elbe");

        boolean result = filter.test(event);

        Assert.assertTrue(result);
    }

    @Test
    public void emptyFileInBlackListModeShouldPassAllValues() {
        TaggedMetricFilter filter = createFilterWithoutRules(false);
        Event event = TaggedEventBuilder.withSingleTag("app", "dagon");

        boolean result = filter.test(event);

        Assert.assertTrue(result);
    }

    @Test
    public void shouldPassAllEventsWithoutTagsInBlackListMode() {
        TaggedMetricFilter filter = createFilter(false);
        Event event = EventBuilder.create()
                .uuid(UUID.randomUUID())
                .timestamp(System.currentTimeMillis())
                .version(1)
                .build();

        boolean result = filter.test(event);

        Assert.assertTrue(result);
    }

    @Test
    public void compositeRuleShouldNotPassIfEventSatisfiesItInBlackListMode() {
        TaggedMetricFilter filter = createFilter(false);
        Event event = createEventSatisfiesCompositeRule().build();

        boolean result = filter.test(event);

        Assert.assertFalse(result);
    }

    @Test
    public void compositeRuleShouldPassIfEventSatisfiesOnlyPartOfItInBlackListMode() {
        TaggedMetricFilter filter = createFilter(false);
        Stream
                .of(
                        createEventSatisfiesCompositeRule()
                                .withTag("app", "some-other-app")
                                .build(),
                        createEventSatisfiesCompositeRule()
                                .withTag("instance", "instance-4")
                                .build(),
                        createEventSatisfiesCompositeRule()
                                .withTag("env", "prod")
                                .build()
                )
                .forEach(event -> {
                    boolean result = filter.test(event);

                    Assert.assertTrue(result);
                });
    }

    @Test
    public void filterInWhiteListModeShouldPassListedValues() {
        TaggedMetricFilter filter = createFilter(true);
        Event event = TaggedEventBuilder.withSingleTag("app", "dagon");

        boolean result = filter.test(event);

        Assert.assertTrue(result);
    }

    @Test
    public void filterInWhiteListModeShouldDenyNotListedValues() {
        TaggedMetricFilter filter = createFilter(true);
        Event event = TaggedEventBuilder.withSingleTag("app", "elbe");

        boolean result = filter.test(event);

        Assert.assertFalse(result);
    }

    @Test
    public void shouldDenyAllEventsWithoutTagsInWhiteListMode() {
        TaggedMetricFilter filter = createFilter(true);
        Event event = EventBuilder.create()
                .uuid(UUID.randomUUID())
                .timestamp(System.currentTimeMillis())
                .version(1)
                .build();

        boolean result = filter.test(event);

        Assert.assertFalse(result);
    }

    @Test
    public void emptyFileInWhiteListModeShouldDenyAllValues() {
        TaggedMetricFilter filter = createFilterWithoutRules(true);
        Event event = TaggedEventBuilder.withSingleTag("app", "dagon");

        boolean result = filter.test(event);

        Assert.assertFalse(result);
    }


    @Test
    public void shouldNotThrowIfUnknownTagMeets() {
        TaggedMetricFilter filter = createFilter(false);
        Event event = TaggedEventBuilder.withSingleTag("some-unknown-tag-name", "value");

        filter.test(event);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldValidateListPathProperty() {
        Properties properties = new Properties();
        properties.put(TaggedMetricFilter.Props.LIST_PATH.name(), "/with/out/file/prefix.tfl");
        properties.put(TaggedMetricFilter.Props.LIST_MATCH_RESULT.name(), false);

        new TaggedMetricFilter(properties);
    }

    private TaggedEventBuilder createEventSatisfiesCompositeRule() {
        return TaggedEventBuilder.create()
                .withTag("app", "ke")
                .withTag("env", "dev")
                .withTag("instance", "instance-3");
    }

    private static TaggedMetricFilter createFilter(boolean matchResult) {
        Properties properties = new Properties();
        properties.put(TaggedMetricFilter.Props.LIST_PATH.name(), "file://src/test/resources/filter-list.tfl");
        properties.put(TaggedMetricFilter.Props.LIST_MATCH_RESULT.name(), Boolean.toString(matchResult));
        return new TaggedMetricFilter(properties);
    }

    private static TaggedMetricFilter createFilterWithoutRules(boolean matchResult) {
        Properties properties = new Properties();
        properties.put(TaggedMetricFilter.Props.LIST_PATH.name(), "file://src/test/resources/empty-filter-list.tfl");
        properties.put(TaggedMetricFilter.Props.LIST_MATCH_RESULT.name(), Boolean.toString(matchResult));
        return new TaggedMetricFilter(properties);
    }

    static class TaggedEventBuilder {
        private final Map<String, String> tags = new HashMap<>();

        static TaggedEventBuilder create() {
            return new TaggedEventBuilder();
        }

        static Event withSingleTag(String tag, String value) {
            return new TaggedEventBuilder()
                    .withTag(tag, value)
                    .build();
        }

        TaggedEventBuilder withTag(String key, String value) {
            tags.put(key, value);
            return this;
        }

        Event build() {
            Container[] tagsContainers = tags.entrySet().stream()
                    .map(entry -> Container.builder()
                            .tag(MetricsTags.TAG_KEY_TAG.getName(), Variant.ofString(entry.getKey()))
                            .tag(MetricsTags.TAG_VALUE_TAG.getName(), Variant.ofString(entry.getValue()))
                            .build()
                    )
                    .toArray(Container[]::new);
            Variant tagsVariant = Variant.ofVector(Vector.ofContainers(tagsContainers));
            return EventBuilder.create()
                    .uuid(UUID.randomUUID())
                    .timestamp(System.currentTimeMillis())
                    .version(1)
                    .tag(MetricsTags.TAGS_VECTOR_TAG.getName(), tagsVariant)
                    .build();
        }
    }
}
