package ru.kontur.vostok.hercules.elastic.sink.index;

import org.junit.Assert;
import org.junit.Test;
import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.EventBuilder;
import ru.kontur.vostok.hercules.protocol.Variant;

import java.util.Optional;
import java.util.Properties;

/**
 * @author Petr Demenev
 */
public class MatchingIndexResolverTest {
    private static final IndexResolver INDEX_RESOLVER;

    static {
        Properties props = new Properties();
        props.setProperty("file", "resource://indices.json");
        INDEX_RESOLVER = new MatchingIndexResolver(props);
    }

    @Test
    public void shouldResolveIndexWithRegex() {
        final Event event = EventBuilder.create(0, "00000000-0000-0000-0000-000000000000").
                tag("some-tag", Variant.ofString("some-value")).
                tag("other-tag", Variant.ofString("other-value-123")).
                tag("outer-tag", Variant.ofContainer(
                        Container.builder().
                        tag("inner-tag", Variant.ofString("inner-value")).
                        build())
                ).
                build();
        Optional<String> index = INDEX_RESOLVER.resolve(event);
        Assert.assertTrue(index.isPresent());
        Assert.assertEquals("first-index", index.get());
    }

    @Test
    public void shouldResolveIndexWithNotStringPatternValues() {
        final Event event = EventBuilder.create(0, "00000000-0000-0000-0000-000000000000").
                tag("number-tag", Variant.ofLong(0L)).
                tag("null-tag", Variant.ofNull()).
                tag("boolean-tag", Variant.ofFlag(true)).
                build();
        Optional<String> index = INDEX_RESOLVER.resolve(event);
        Assert.assertTrue(index.isPresent());
        Assert.assertEquals("first-index", index.get());
    }

    @Test
    public void shouldNotResolveIndex() {
        final Event event = EventBuilder.create(0, "00000000-0000-0000-0000-000000000000").
                tag("some-tag", Variant.ofString("some-value")).
                tag("other-tag", Variant.ofString("other-value")).
                tag("outer-tag", Variant.ofContainer(
                        Container.builder().
                                tag("inner-tag", Variant.ofString("inner-value")).
                                build())
                ).
                build();
        Optional<String> index = INDEX_RESOLVER.resolve(event);
        Assert.assertFalse(index.isPresent());
    }

    @Test
    public void shouldResolveSecondIndex() {
        final Event event = EventBuilder.create(0, "00000000-0000-0000-0000-000000000000").
                tag("foo", Variant.ofInteger(1)).
                tag("bar", Variant.ofString("bar-value")).
                tag("other-tag", Variant.ofString("other-value")).
                build();
        Optional<String> index = INDEX_RESOLVER.resolve(event);
        Assert.assertTrue(index.isPresent());
        Assert.assertEquals("second-index", index.get());
    }

    @Test
    public void shouldResolveFirstIndexWhenEventMatchesToTwoIndices() {
        final Event event = EventBuilder.create(0, "00000000-0000-0000-0000-000000000000").
                tag("foo", Variant.ofInteger(1)).
                tag("bar", Variant.ofString("bar-value")).
                tag("some-tag", Variant.ofString("some-value")).
                tag("other-tag", Variant.ofString("other-value-123")).
                tag("outer-tag", Variant.ofContainer(
                        Container.builder().
                                tag("inner-tag", Variant.ofString("inner-value")).
                                build())
                ).
                build();
        Optional<String> index = INDEX_RESOLVER.resolve(event);
        Assert.assertTrue(index.isPresent());
        Assert.assertEquals("first-index", index.get());
    }
}
