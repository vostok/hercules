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

import static org.junit.Assert.assertFalse;

/**
 * @author Gregory Koshelev
 */
public class IndexResolverTest {
    @Test
    public void shoulNotResolveIndexIfNoSuitableTags() {
        IndexResolver indexResolver = IndexResolver.forPolicy(IndexPolicy.DAILY);
        final Event event = EventBuilder.create(0, "00000000-0000-1000-994f-8fcf383f0000") //TODO: fix me!
                .build();
        Optional<String> result = indexResolver.resolve(event);
        assertFalse(result.isPresent());
    }

    @Test
    public void shouldResolveDailyIndex() {
        IndexResolver indexResolver = IndexResolver.forPolicy(IndexPolicy.DAILY);
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
        IndexResolver indexResolver = IndexResolver.forPolicy(IndexPolicy.ILM);
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
}
