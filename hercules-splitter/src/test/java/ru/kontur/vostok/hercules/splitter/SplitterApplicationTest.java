package ru.kontur.vostok.hercules.splitter;

import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.TestInputTopic;
import org.apache.kafka.streams.TestOutputTopic;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.TopologyTestDriver;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import ru.kontur.vostok.hercules.kafka.util.serialization.EventDeserializer;
import ru.kontur.vostok.hercules.kafka.util.serialization.EventSerializer;
import ru.kontur.vostok.hercules.kafka.util.serialization.UuidDeserializer;
import ru.kontur.vostok.hercules.kafka.util.serialization.UuidSerializer;
import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.EventBuilder;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.protocol.Vector;
import ru.kontur.vostok.hercules.tags.MetricsTags;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;
import ru.kontur.vostok.hercules.util.time.TimeUtil;

import java.time.Instant;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

/**
 * Application test class.
 *
 * @author Aleksandr Yuferov
 */
@RunWith(Enclosed.class)
public class SplitterApplicationTest {

    /**
     * Kafka Streams {@link Topology} configuration test.
     */
    public static class TopologyTest {

        private TopologyTestDriver driver;
        private TestInputTopic<UUID, Event> inputTopic;
        private TestOutputTopic<UUID, Event> outputTopic;

        /**
         * Preparations for tests.
         */
        @Before
        public void prepare() {
            Topology topology = new SplitterApplication().createTopology(PropertiesUtil.ofEntries(
                    Map.entry(ConfigurationProperties.SHARDING_KEYS.name(), "tags"),
                    Map.entry(ConfigurationProperties.HASH_ALGORITHM.name(), "xxhash32_fastest_java_instance"),
                    Map.entry(ConfigurationProperties.PATTERN.name(), "input_topic"),
                    Map.entry(ConfigurationProperties.OUTPUT_STREAM_TEMPLATE.name(), "output_topic_<node>"),
                    Map.entry(ConfigurationProperties.PARTITION_WEIGHTS_SCOPE + ".node_1", "1.0")
            ));

            Properties driverProperties = PropertiesUtil.ofEntries(
                    Map.entry(StreamsConfig.APPLICATION_ID_CONFIG, "test"),
                    Map.entry(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "dummy:1234")
            );

            driver = new TopologyTestDriver(topology, driverProperties);
            inputTopic = driver.createInputTopic("input_topic", new UuidSerializer(), new EventSerializer());
            outputTopic = driver.createOutputTopic("output_topic_node_1", new UuidDeserializer(),
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

        @Test
        public void taggedMetricTest() {
            Event taggedMetricEvent = EventBuilder.create(TimeUtil.millisToTicks(Instant.now().toEpochMilli()), UUID.randomUUID())
                    .tag(MetricsTags.TAGS_VECTOR_TAG.getName(), Variant.ofVector(Vector.ofContainers(
                            Container.builder()
                                    .tag(MetricsTags.TAG_KEY_TAG.getName(), Variant.ofString("foo"))
                                    .tag(MetricsTags.TAG_VALUE_TAG.getName(), Variant.ofString("foobar"))
                                    .build(),
                            Container.builder()
                                    .tag(MetricsTags.TAG_KEY_TAG.getName(), Variant.ofString("bar"))
                                    .tag(MetricsTags.TAG_VALUE_TAG.getName(), Variant.ofString("foobar"))
                                    .build()
                    )))
                    .tag(MetricsTags.METRIC_VALUE_TAG.getName(), Variant.ofDouble(42))
                    .build();

            inputTopic.pipeInput(taggedMetricEvent);

            Assert.assertFalse(outputTopic.isEmpty());
            Assert.assertEquals(1, outputTopic.getQueueSize());
            Event sentEvent = outputTopic.readValue();
            Assert.assertEquals(taggedMetricEvent.getUuid(), sentEvent.getUuid());
        }

        @Test
        public void flatMetricTest() {
            Event taggedMetricEvent = EventBuilder.create(TimeUtil.millisToTicks(Instant.now().toEpochMilli()), UUID.randomUUID())
                    .tag(MetricsTags.TAGS_VECTOR_TAG.getName(), Variant.ofVector(Vector.ofContainers(
                            Container.builder()
                                    .tag(MetricsTags.TAG_KEY_TAG.getName(), Variant.ofString("_name"))
                                    .tag(MetricsTags.TAG_VALUE_TAG.getName(), Variant.ofString("foo.bar.baz"))
                                    .build()
                    )))
                    .tag(MetricsTags.METRIC_VALUE_TAG.getName(), Variant.ofDouble(42))
                    .build();

            inputTopic.pipeInput(taggedMetricEvent);

            Assert.assertFalse(outputTopic.isEmpty());
            Assert.assertEquals(1, outputTopic.getQueueSize());
            Event sentEvent = outputTopic.readValue();
            Assert.assertEquals(taggedMetricEvent.getUuid(), sentEvent.getUuid());
        }

        @Test
        public void badDataTest() {
            Event badData = new Event(new byte[10], 1, 0, UUID.randomUUID(), Container.builder().build());

            inputTopic.pipeInput(badData);

            Assert.assertTrue(outputTopic.isEmpty());
        }
    }
}
