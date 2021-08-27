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
public class StaticIndexResolverTest {

    @Test
    public void shouldResolveIndex() {
        Properties props = new Properties();
        props.setProperty("index.name", "static-index");
        StaticIndexResolver indexResolver = new StaticIndexResolver(props);
        final Event event = EventBuilder.create(
                TimeUtil.dateTimeToUnixTicks(ZonedDateTime.of(2019, 12, 1, 10, 42, 0, 0, ZoneOffset.UTC)),
                "00000000-0000-0000-0000-000000000000").
                tag("properties", Variant.ofContainer(
                        Container.builder().
                                tag("elk-index", Variant.ofString("custom-index")).
                                build())).
                build();

        Optional<String> index = indexResolver.resolve(event);
        Assert.assertTrue(index.isPresent());
        Assert.assertEquals("static-index", index.get());
    }
}
