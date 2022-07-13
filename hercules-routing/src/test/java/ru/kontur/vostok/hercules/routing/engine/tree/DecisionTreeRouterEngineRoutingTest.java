package ru.kontur.vostok.hercules.routing.engine.tree;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
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
@RunWith(Parameterized.class)
public class DecisionTreeRouterEngineRoutingTest {
    private final DecisionTreeEngineConfig config = DecisionTreeEngineConfig.builder()
            .addAllowedTag("tag1")
            .addAllowedTag("tag2")
            .addAllowedTag("tag3")
            .addAllowedTag("tag4")
            .build();

    private final TestDestination defaultDestination = TestDestination.of("default destination");

    @Parameterized.Parameter
    public List<DecisionTreeEngineRoute<TestDestination>> rules;

    @Parameterized.Parameter(1)
    public Event event;

    @Parameterized.Parameter(2)
    public String expectedDestination;

    @Test
    public void test() {
        var configTask = new ConstantConfigurationWatchTask(config, rules);
        var engine = new DecisionTreeRouterEngine(config, defaultDestination);

        configTask.start(engine);
        TestDestination actualDestination = (TestDestination) engine.route(event);

        Assert.assertEquals(expectedDestination, actualDestination.value());
    }

    @Parameterized.Parameters
    public static Object[][] parameters() {
        List<DecisionTreeEngineRoute<TestDestination>> oneRule = List.of(
                DecisionTreeEngineRoute.<TestDestination>builder()
                        .setConditions(Map.of(
                                "tag1", "value1",
                                "tag2", "value2",
                                "tag3", "value3"
                        ))
                        .setDestination(TestDestination.of("destination 1"))
                        .build()
        );
        List<DecisionTreeEngineRoute<TestDestination>> nestedRules = List.of(
                DecisionTreeEngineRoute.<TestDestination>builder()
                        .setConditions(Map.of(
                                "tag1", "value1"
                        ))
                        .setDestination(TestDestination.of("destination 0"))
                        .build(),
                DecisionTreeEngineRoute.<TestDestination>builder()
                        .setConditions(Map.of(
                                "tag1", "value1",
                                "tag2", "value2"
                        ))
                        .setDestination(TestDestination.of("destination 1"))
                        .build(),
                DecisionTreeEngineRoute.<TestDestination>builder()
                        .setConditions(Map.of(
                                "tag1", "value1",
                                "tag2", "value2",
                                "tag3", "value3"
                        ))
                        .setDestination(TestDestination.of("destination 2"))
                        .build()
        );
        return new Object[][]{
                {
                        List.of(),
                        EventBuilder.create(0, UUID.randomUUID()).build(),
                        "default destination"
                },
                {
                        oneRule,
                        EventBuilder.create(0, UUID.randomUUID())
                                .tag("tag1", Variant.ofString("value1"))
                                .tag("tag2", Variant.ofString("value2"))
                                .tag("tag3", Variant.ofString("value3"))
                                .build(),
                        "destination 1"
                },
                {
                        oneRule,
                        EventBuilder.create(0, UUID.randomUUID())
                                .tag("tag1", Variant.ofString("value1"))
                                .tag("tag2", Variant.ofString("value2"))
                                .tag("tag3", Variant.ofString("other-value"))
                                .build(),
                        "default destination"
                },
                {
                        oneRule,
                        EventBuilder.create(0, UUID.randomUUID())
                                .tag("tag1", Variant.ofString("value1"))
                                .tag("tag2", Variant.ofString("value-other"))
                                .tag("tag3", Variant.ofString("other3"))
                                .build(),
                        "default destination"
                },
                {
                        oneRule,
                        EventBuilder.create(0, UUID.randomUUID())
                                .tag("tag1", Variant.ofString("value-other"))
                                .tag("tag2", Variant.ofString("value2"))
                                .tag("tag3", Variant.ofString("other3"))
                                .build(),
                        "default destination"
                },
                {
                        oneRule,
                        EventBuilder.create(0, UUID.randomUUID())
                                .tag("tag1", Variant.ofString("value1"))
                                .tag("tag2", Variant.ofString("value2"))
                                .build(),
                        "default destination"
                },
                {
                        oneRule,
                        EventBuilder.create(0, UUID.randomUUID())
                                .tag("tag1", Variant.ofString("value1"))
                                .tag("tag3", Variant.ofString("value3"))
                                .build(),
                        "default destination"
                },
                {
                        oneRule,
                        EventBuilder.create(0, UUID.randomUUID())
                                .tag("tag2", Variant.ofString("value2"))
                                .tag("tag3", Variant.ofString("value3"))
                                .build(),
                        "default destination"
                },
                {
                        List.of(
                                DecisionTreeEngineRoute.<TestDestination>builder()
                                        .setConditions(Map.of(
                                                "tag1", "value1",
                                                "tag3", "value3"
                                        ))
                                        .setDestination(TestDestination.of("destination 1"))
                                        .build()
                        ),
                        EventBuilder.create(0, UUID.randomUUID())
                                .tag("tag1", Variant.ofString("value1"))
                                .tag("tag2", Variant.ofString("value2"))
                                .tag("tag3", Variant.ofString("value3"))
                                .build(),
                        "default destination"
                },
                {
                        List.of(
                                DecisionTreeEngineRoute.<TestDestination>builder()
                                        .setConditions(Map.of(
                                                "tag1", "value1",
                                                "tag3", "value3"
                                        ))
                                        .setDestination(TestDestination.of("destination 1"))
                                        .build()
                        ),
                        EventBuilder.create(0, UUID.randomUUID())
                                .tag("tag1", Variant.ofString("value1"))
                                .tag("tag3", Variant.ofString("value3"))
                                .build(),
                        "destination 1"
                },
                {
                        nestedRules,
                        EventBuilder.create(0, UUID.randomUUID())
                                .tag("tag1", Variant.ofString("value1"))
                                .tag("tag2", Variant.ofString("value2"))
                                .tag("tag3", Variant.ofString("value3"))
                                .build(),
                        "destination 2"
                },
                {
                        nestedRules,
                        EventBuilder.create(0, UUID.randomUUID())
                                .tag("tag1", Variant.ofString("value1"))
                                .tag("tag2", Variant.ofString("value2"))
                                .tag("tag3", Variant.ofString("value-other"))
                                .build(),
                        "destination 1"
                },
                {
                        nestedRules,
                        EventBuilder.create(0, UUID.randomUUID())
                                .tag("tag1", Variant.ofString("value1"))
                                .tag("tag2", Variant.ofString("value2"))
                                .build(),
                        "destination 1"
                },
                {
                        nestedRules,
                        EventBuilder.create(0, UUID.randomUUID())
                                .tag("tag1", Variant.ofString("value1"))
                                .tag("tag3", Variant.ofString("value3"))
                                .build(),
                        "destination 0"
                },
                {
                        nestedRules,
                        EventBuilder.create(0, UUID.randomUUID())
                                .tag("tag1", Variant.ofString("value1"))
                                .tag("tag2", Variant.ofString("value-other"))
                                .build(),
                        "destination 0"
                },
                {
                        nestedRules,
                        EventBuilder.create(0, UUID.randomUUID())
                                .tag("tag1", Variant.ofString("value-other"))
                                .build(),
                        "default destination"
                }
        };
    }
}
