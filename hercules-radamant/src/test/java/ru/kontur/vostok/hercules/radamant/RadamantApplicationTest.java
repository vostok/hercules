package ru.kontur.vostok.hercules.radamant;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import org.apache.kafka.streams.TestInputTopic;
import org.apache.kafka.streams.TestOutputTopic;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.TopologyTestDriver;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mockito;
import ru.kontur.vostok.hercules.kafka.util.serialization.EventDeserializer;
import ru.kontur.vostok.hercules.kafka.util.serialization.EventSerializer;
import ru.kontur.vostok.hercules.kafka.util.serialization.UuidDeserializer;
import ru.kontur.vostok.hercules.kafka.util.serialization.UuidSerializer;
import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.EventBuilder;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.protocol.Vector;
import ru.kontur.vostok.hercules.radamant.rules.RuleMatch;
import ru.kontur.vostok.hercules.radamant.rules.RuleMatches;
import ru.kontur.vostok.hercules.radamant.rules.RuleTable;
import ru.kontur.vostok.hercules.tags.MetricsTags;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;
import ru.kontur.vostok.hercules.util.time.TimeUtil;

/**
 * Application test class
 *
 * @author Tatyana Tokmyanina
 */
@RunWith(Parameterized.class)
public class RadamantApplicationTest {

    private Event eventFlat;
    private Event enrichedEvent;
    private RadamantApplication application;

    private TopologyTestDriver driver;
    private TestInputTopic<UUID, Event> inputTopic;
    private TestOutputTopic<UUID, Event> aggregatedTopic;

    private final int countOfRules;

    public RadamantApplicationTest(int countOfRules) {
        super();
        this.countOfRules = countOfRules;
    }

    /**
     * Preparations for tests.
     */
    @Before
    public void prepare() {
        eventFlat = EventBuilder.create(TimeUtil.millisToTicks(Instant.now().toEpochMilli()),
                        UUID.randomUUID())
                .tag(MetricsTags.TAGS_VECTOR_TAG.getName(), Variant.ofVector(Vector.ofContainers(
                        Container.builder()
                                .tag(MetricsTags.TAG_KEY_TAG.getName(), Variant.ofString("_name"))
                                .tag(MetricsTags.TAG_VALUE_TAG.getName(),
                                        Variant.ofString("foo.bar.baz"))
                                .build()
                )))
                .tag(MetricsTags.METRIC_VALUE_TAG.getName(), Variant.ofDouble(42))
                .build();

        enrichedEvent = EventBuilder.create(TimeUtil.millisToTicks(Instant.now().toEpochMilli()),
                        UUID.randomUUID())
                .tag(MetricsTags.TAGS_VECTOR_TAG.getName(), Variant.ofVector(Vector.ofContainers(
                        Container.builder()
                                .tag(MetricsTags.TAG_KEY_TAG.getName(), Variant.ofString("_name"))
                                .tag(MetricsTags.TAG_VALUE_TAG.getName(), Variant.ofString("foo.bar.baz"))
                                .build()
                )))
                .tag(MetricsTags.METRIC_VALUE_TAG.getName(), Variant.ofDouble(42))
                .tag(MetricsTags.ENRICHMENT_TAG.getName(), Variant.ofContainer(
                        Container.builder()
                                .tag(MetricsTags.NEW_NAME_TAG.getName(), Variant.ofString("foo.bar"))
                                .build()
                ))
                .build();

        application = new RadamantApplication();

        Topology topology = application.createTopology(PropertiesUtil.ofEntries(
                Map.entry(ConfigurationProperties.PATTERN.name(), "metrics"),
                Map.entry(ConfigurationProperties.DEFAULT_RESULT_STREAM.name(),
                        "metrics_for_aggregation")
        ));

        Properties driverProperties = PropertiesUtil.ofEntries();

        driver = new TopologyTestDriver(topology, driverProperties);
        inputTopic = driver.createInputTopic("metrics", new UuidSerializer(),
                new EventSerializer());
        aggregatedTopic = driver.createOutputTopic(
                "metrics_for_aggregation",
                new UuidDeserializer(),
                EventDeserializer.parseNoTags());
    }

    /**
     * After tests cleanup.
     */
    @After
    public void cleanup() {
        if (driver != null) {
            driver.close();
        }
    }

    @Parameters
    public static Collection<Object> data() {
        return Arrays.asList(new Object[]{1, 2, 0});
    }

    @Test
    public void metricTest() {
        RuleTable ruleTable = Mockito.mock(RuleTable.class);
        List<RuleMatch> ruleMatchesList = new ArrayList<>();
        for (int i = 0; i < countOfRules; i++) {
            ruleMatchesList.add(new RuleMatch(null, null, null));
        }
        RuleMatches ruleMatches = new RuleMatches(eventFlat, ruleMatchesList);
        Mockito.when(ruleTable.navigate(any())).thenReturn(ruleMatches);

        Enricher enricher = Mockito.mock(Enricher.class);
        Mockito.when(enricher.enrich(any(), any())).thenReturn(enrichedEvent);

        application.setRuleTableFlat(ruleTable);
        application.setEnricher(enricher);
        inputTopic.pipeInput(UUID.randomUUID(), eventFlat);

        assertEquals(countOfRules, aggregatedTopic.getQueueSize());
    }
}
