package ru.kontur.vostok.hercules.routing.engine.tree;

import org.junit.Assert;
import org.junit.Test;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.EventBuilder;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.routing.engine.TestDestination;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @author Aleksandr Yuferov
 */
public class DecisionTreeRouterEngineInterpolationTest {
    private final DecisionTreeEngineConfig config = DecisionTreeEngineConfig.builder()
            .addAllowedTag("tag1")
            .addAllowedTag("tag2")
            .build();

    private final TestDestination defaultDestination = TestDestination.of("{tag:tag1}");

    private final List<DecisionTreeEngineRoute<TestDestination>> routes = List.of(
            DecisionTreeEngineRoute.<TestDestination>builder()
                    .setConditions(Map.of(
                            "tag1", "value1"
                    ))
                    .setDestination(TestDestination.of("{tag:tag2}"))
                    .build()
    );

    private final DecisionTreeRouterEngine engine = new DecisionTreeRouterEngine(config, defaultDestination);

    @Test
    public void defaultDestination() {
        engine.init(config, routes);
        Event event = EventBuilder.create(0, UUID.randomUUID())
                .tag("tag1", Variant.ofString("value2"))
                .build();

        var destination = (TestDestination) engine.route(event);

        Assert.assertEquals("value2", destination.value());
    }

    @Test
    public void notDefaultDestination() {
        engine.init(config, routes);
        Event event = EventBuilder.create(0, UUID.randomUUID())
                .tag("tag1", Variant.ofString("value1"))
                .tag("tag2", Variant.ofString("some-value"))
                .build();

        var destination = (TestDestination) engine.route(event);

        Assert.assertEquals("some-value", destination.value());
    }
}
