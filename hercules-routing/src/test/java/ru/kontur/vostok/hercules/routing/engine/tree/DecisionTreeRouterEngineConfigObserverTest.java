package ru.kontur.vostok.hercules.routing.engine.tree;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.EventBuilder;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.routing.config.constant.ConstantConfigurationWatchTask;
import ru.kontur.vostok.hercules.routing.engine.TestDestination;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @author Aleksandr Yuferov
 */
public class DecisionTreeRouterEngineConfigObserverTest {
    private final DecisionTreeEngineConfig defaultConfig = DecisionTreeEngineConfig.builder()
            .addAllowedTag("tag1")
            .addAllowedTag("tag2")
            .build();

    private final DecisionTreeEngineRoute<TestDestination> storedRoute = DecisionTreeEngineRoute
            .<TestDestination>builder()
            .setId(UUID.randomUUID())
            .setConditions(Map.of(
                    "tag1", "special-value-1",
                    "tag2", "special-value-2"
            ))
            .setDestination(TestDestination.of("special destination"))
            .build();

    private final ConstantConfigurationWatchTask configWatchTask
            = new ConstantConfigurationWatchTask(defaultConfig, List.of(storedRoute));

    private final DecisionTreeRouterEngine engine
            = new DecisionTreeRouterEngine(defaultConfig, TestDestination.of("default destination"));

    @Before
    public void prepare() {
        configWatchTask.start(engine);
    }

    @Test
    public void shouldHandleEventCreation() {
        Event event = EventBuilder.create(0, UUID.randomUUID())
                .tag("tag1", Variant.ofString("value1"))
                .tag("tag2", Variant.ofString("value2"))
                .build();

        assertThatDestinationForEventWillBe(event, "default destination");

        configWatchTask.addRoute(DecisionTreeEngineRoute.<TestDestination>builder()
                .setConditions(Map.of(
                        "tag1", "value1",
                        "tag2", "value2"
                ))
                .setDestination(TestDestination.of("destination 1"))
                .build());

        assertThatDestinationForEventWillBe(event, "destination 1");
    }

    @Test
    public void shouldHandleRouteRemove() {
        Event event = EventBuilder.create(0, UUID.randomUUID())
                .tag("tag1", Variant.ofString("special-value-1"))
                .tag("tag2", Variant.ofString("special-value-2"))
                .build();

        assertThatDestinationForEventWillBe(event, "special destination");

        configWatchTask.removeRoute(storedRoute.id());

        assertThatDestinationForEventWillBe(event, "default destination");
    }

    @Test
    public void shouldHandleRouteChange() {
        Event event = EventBuilder.create(0, UUID.randomUUID())
                .tag("tag1", Variant.ofString("special-value-1"))
                .tag("tag2", Variant.ofString("special-value-2"))
                .build();

        assertThatDestinationForEventWillBe(event, "special destination");

        Event appropriateEvent = EventBuilder.create(0, UUID.randomUUID())
                .tag("tag1", Variant.ofString("special-value-1"))
                .tag("tag2", Variant.ofString("another-special-value-2"))
                .build();
        DecisionTreeEngineRoute<TestDestination> routeNewState = DecisionTreeEngineRoute
                .<TestDestination>builder()
                .setId(storedRoute.id())
                .setConditions(Map.of(
                        "tag1", "special-value-1",
                        "tag2", "another-special-value-2"
                ))
                .setDestination(TestDestination.of("another special destination"))
                .build();

        configWatchTask.changeRoute(routeNewState);

        assertThatDestinationForEventWillBe(event, "default destination");
        assertThatDestinationForEventWillBe(appropriateEvent, "another special destination");
    }

    @Test
    public void shouldHandleEngineConfigChanges() {
        Event event1 = EventBuilder.create(0, UUID.randomUUID())
                .tag("tag1", Variant.ofString("special-value-1"))
                .tag("tag2", Variant.ofString("special-value-2"))
                .build();
        Event event2 = EventBuilder.create(0, UUID.randomUUID())
                .tag("tag1", Variant.ofString("special-value-1"))
                .tag("tag2", Variant.ofString("any-other-value"))
                .build();

        assertThatDestinationForEventWillBe(event1, "special destination");
        assertThatDestinationForEventWillBe(event2, "default destination");

        configWatchTask.changeEngineConfig(DecisionTreeEngineConfig.builder()
                .addAllowedTag("tag1")
                .build());

        assertThatDestinationForEventWillBe(event1, "special destination");
        assertThatDestinationForEventWillBe(event2, "special destination");
    }

    private void assertThatDestinationForEventWillBe(Event event, String expectedDestination) {
        var actualDestination = (TestDestination) engine.route(event);

        Assert.assertEquals(expectedDestination, actualDestination.value());
    }
}
