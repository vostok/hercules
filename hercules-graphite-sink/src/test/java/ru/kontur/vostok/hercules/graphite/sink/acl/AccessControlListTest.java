package ru.kontur.vostok.hercules.graphite.sink.acl;

import org.junit.Assert;
import org.junit.Test;
import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.EventBuilder;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.protocol.Vector;
import ru.kontur.vostok.hercules.tags.MetricsTags;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @author Vladimir Tsypaev
 */
public class AccessControlListTest {

    @Test
    public void shouldCheckAllPatternElements() {
        final List<AccessControlEntry> list = new ArrayList<>();
        list.add(AccessControlEntry.fromString("PERMIT value.second_value.third_value"));
        final Statement statement = Statement.DENY;
        final AccessControlList acl = new AccessControlList(list, statement);

        Event event = createMetricEvent();

        Assert.assertTrue(acl.isPermit(event));
    }

    @Test
    public void shouldCheckPatternWithStarAtTheEnd() {
        final List<AccessControlEntry> list = new ArrayList<>();
        list.add(AccessControlEntry.fromString("DENY value.*"));
        final Statement statement = Statement.PERMIT;
        final AccessControlList acl = new AccessControlList(list, statement);

        Event event = createMetricEvent();

        Assert.assertFalse(acl.isPermit(event));
    }

    @Test
    public void shouldCheckPatternWithStarInTheMiddle() {
        final List<AccessControlEntry> list = new ArrayList<>();
        list.add(AccessControlEntry.fromString("DENY value.*.third_value"));
        final Statement statement = Statement.PERMIT;
        final AccessControlList acl = new AccessControlList(list, statement);

        Event event = createMetricEvent();

        Assert.assertFalse(acl.isPermit(event));
    }

    @Test
    public void shouldCheckPatternWithStarAtTheBeginning() {
        final List<AccessControlEntry> list = new ArrayList<>();
        list.add(AccessControlEntry.fromString("DENY *.second_value.third_value"));
        final Statement statement = Statement.PERMIT;
        final AccessControlList acl = new AccessControlList(list, statement);

        Event event = createMetricEvent();

        Assert.assertFalse(acl.isPermit(event));
    }

    @Test
    public void shouldCheckPatternOnlyWithStar() {
        final List<AccessControlEntry> list = new ArrayList<>();
        list.add(AccessControlEntry.fromString("DENY *"));
        final Statement statement = Statement.PERMIT;
        final AccessControlList acl = new AccessControlList(list, statement);

        Event event = createMetricEvent();

        Assert.assertFalse(acl.isPermit(event));
    }

    @Test
    public void shouldCheckPatternWithFewStar() {
        final List<AccessControlEntry> list = new ArrayList<>();
        list.add(AccessControlEntry.fromString("DENY *.*.third_value"));
        final Statement statement = Statement.PERMIT;
        final AccessControlList acl = new AccessControlList(list, statement);

        Event event = createMetricEvent();

        Assert.assertFalse(acl.isPermit(event));
    }

    @Test
    public void shouldCheckStarWithSuffixPatternSegment() {
        final List<AccessControlEntry> list = new ArrayList<>();
        list.add(AccessControlEntry.fromString("DENY *suffix"));
        final Statement statement = Statement.PERMIT;
        final AccessControlList acl = new AccessControlList(list, statement);

        final Event event = EventBuilder.create(0, UUID.randomUUID())
                .tag(MetricsTags.TAGS_VECTOR_TAG.getName(), Variant.ofVector(Vector.ofContainers(
                        Container.builder()
                                .tag(MetricsTags.TAG_KEY_TAG.getName(), Variant.ofString("key"))
                                .tag(MetricsTags.TAG_VALUE_TAG.getName(), Variant.ofString("value_suffix"))
                                .build()
                )))
                .tag(MetricsTags.METRIC_VALUE_TAG.getName(), Variant.ofDouble(1.5d))
                .build();

        Assert.assertFalse(acl.isPermit(event));
    }

    @Test
    public void shouldCheckStarWithPrefixPatternSegment() {
        final List<AccessControlEntry> list = new ArrayList<>();
        list.add(AccessControlEntry.fromString("DENY prefix*"));
        final Statement statement = Statement.PERMIT;
        final AccessControlList acl = new AccessControlList(list, statement);

        final Event event = EventBuilder.create(0, UUID.randomUUID())
                .tag(MetricsTags.TAGS_VECTOR_TAG.getName(), Variant.ofVector(Vector.ofContainers(
                        Container.builder()
                                .tag(MetricsTags.TAG_KEY_TAG.getName(), Variant.ofString("key"))
                                .tag(MetricsTags.TAG_VALUE_TAG.getName(), Variant.ofString("prefix_value"))
                                .build()
                )))
                .tag(MetricsTags.METRIC_VALUE_TAG.getName(), Variant.ofDouble(1.5d))
                .build();

        Assert.assertFalse(acl.isPermit(event));
    }

    @Test
    public void shouldCheckStarWithPrefixAndSuffixPatternSegment() {
        final List<AccessControlEntry> list = new ArrayList<>();
        list.add(AccessControlEntry.fromString("DENY prefix*"));
        final Statement statement = Statement.PERMIT;
        final AccessControlList acl = new AccessControlList(list, statement);

        final Event event = EventBuilder.create(0, UUID.randomUUID())
                .tag(MetricsTags.TAGS_VECTOR_TAG.getName(), Variant.ofVector(Vector.ofContainers(
                        Container.builder()
                                .tag(MetricsTags.TAG_KEY_TAG.getName(), Variant.ofString("key"))
                                .tag(MetricsTags.TAG_VALUE_TAG.getName(), Variant.ofString("prefix_value_suffix"))
                                .build()
                )))
                .tag(MetricsTags.METRIC_VALUE_TAG.getName(), Variant.ofDouble(1.5d))
                .build();

        Assert.assertFalse(acl.isPermit(event));
    }

    @Test
    public void shouldCheckFewPatterns() {
        final List<AccessControlEntry> list = new ArrayList<>();
        list.add(AccessControlEntry.fromString("DENY value.second_value.third_value.fourth_value"));
        list.add(AccessControlEntry.fromString("PERMIT value.second_value.third_value.*"));
        list.add(AccessControlEntry.fromString("DENY value.second_value.*"));
        final Statement statement = Statement.PERMIT;
        final AccessControlList acl = new AccessControlList(list, statement);

        Event event = createMetricEvent();

        Assert.assertFalse(acl.isPermit(event));
    }

    @Test
    public void shouldReturnDefaultStatement() {
        final List<AccessControlEntry> list = new ArrayList<>();
        list.add(AccessControlEntry.fromString("PERMIT other_value.*"));
        final Statement statement = Statement.DENY;
        AccessControlList acl = new AccessControlList(list, statement);

        Event event = createMetricEvent();

        Assert.assertFalse("No pattern matches", acl.isPermit(event));


        list.clear();
        list.add(AccessControlEntry.fromString("PERMIT value.second_value.third_value.fourth_value"));
        acl = new AccessControlList(list, statement);

        Assert.assertFalse("Pattern element more then metric path", acl.isPermit(event));


        list.clear();
        list.add(AccessControlEntry.fromString("PERMIT value.second_value"));
        acl = new AccessControlList(list, statement);

        Assert.assertFalse("Pattern does not contain STAR at the end and pattern element less then metric path",
                acl.isPermit(event));
    }

    private Event createMetricEvent() {
        return EventBuilder.create(0, UUID.randomUUID())
                .tag(MetricsTags.TAGS_VECTOR_TAG.getName(), Variant.ofVector(Vector.ofContainers(
                        Container.builder()
                                .tag(MetricsTags.TAG_KEY_TAG.getName(), Variant.ofString("key"))
                                .tag(MetricsTags.TAG_VALUE_TAG.getName(), Variant.ofString("value"))
                                .build(),
                        Container.builder()
                                .tag(MetricsTags.TAG_KEY_TAG.getName(), Variant.ofString("second_key"))
                                .tag(MetricsTags.TAG_VALUE_TAG.getName(), Variant.ofString("second_value"))
                                .build(),
                        Container.builder()
                                .tag(MetricsTags.TAG_KEY_TAG.getName(), Variant.ofString("third_key"))
                                .tag(MetricsTags.TAG_VALUE_TAG.getName(), Variant.ofString("third_value"))
                                .build()
                )))
                .tag(MetricsTags.METRIC_VALUE_TAG.getName(), Variant.ofDouble(1.5d))
                .build();
    }
}