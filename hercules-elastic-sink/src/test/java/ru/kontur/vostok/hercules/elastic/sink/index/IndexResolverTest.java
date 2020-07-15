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
        IndexResolver indexResolver = LogEventIndexResolver.forPolicy(IndexPolicy.DAILY);
        final Event event = EventBuilder.create(0, "00000000-0000-1000-994f-8fcf383f0000") //TODO: fix me!
                .build();
        Optional<String> result = indexResolver.resolve(event);
        assertFalse(result.isPresent());
    }

    @Test
    public void shouldResolveDailyIndex() {
        IndexResolver indexResolver = LogEventIndexResolver.forPolicy(IndexPolicy.DAILY);
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
        IndexResolver indexResolver = LogEventIndexResolver.forPolicy(IndexPolicy.ILM);
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
        IndexResolver indexResolver = LogEventIndexResolver.forPolicy(IndexPolicy.DAILY);
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
        IndexResolver indexResolver = LogEventIndexResolver.forPolicy(IndexPolicy.DAILY);
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
}
