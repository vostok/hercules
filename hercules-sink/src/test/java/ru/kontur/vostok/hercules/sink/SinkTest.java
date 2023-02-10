package ru.kontur.vostok.hercules.sink;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.MockConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;
import org.apache.kafka.common.TopicPartition;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.kontur.vostok.hercules.application.Application;
import ru.kontur.vostok.hercules.application.ApplicationRunner;
import ru.kontur.vostok.hercules.health.NoOpMetricsCollector;
import ru.kontur.vostok.hercules.kafka.util.consumer.Subscription;
import ru.kontur.vostok.hercules.kafka.util.serialization.EventDeserializer;
import ru.kontur.vostok.hercules.kafka.util.serialization.UuidDeserializer;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.sink.metrics.SinkMetrics;
import ru.kontur.vostok.hercules.sink.parallel.TestPreparedData;
import ru.kontur.vostok.hercules.sink.parallel.TestUtils;
import ru.kontur.vostok.hercules.sink.parallel.sender.ParallelSender;

import java.util.Collections;
import java.util.HashMap;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.kontur.vostok.hercules.sink.parallel.TestUtils.TOPIC;

@ExtendWith(MockitoExtension.class)
public class SinkTest {
    MockConsumer<UUID, Event> consumer;

    @Mock
    ParallelSender<TestPreparedData> parallelSender;

    @Mock
    ApplicationRunner applicationRunner;

    Application app;

    @BeforeEach
    public void setUp() {
        app = Application.run(applicationRunner, "application.properties=resource://base.properties");
        consumer = new MockConsumer<>(OffsetResetStrategy.EARLIEST);
    }

    @AfterEach
    public void afterAll() {
        app.stop();
    }

    @Test
    void start() {
        TopicPartition topicPartition = new TopicPartition(TOPIC, 0);
        Mockito.when(parallelSender.isAvailable()).thenReturn(true);
        Mockito.when(parallelSender.process(Mockito.any())).thenReturn(ProcessorResult.ok(2, 0));

        ConsumerRecord<UUID, Event> record1 = TestUtils.eventRecord(topicPartition, TestUtils.createEvent());
        ConsumerRecord<UUID, Event> record2 = TestUtils.eventRecord(topicPartition, TestUtils.createEvent());
        consumer.updateBeginningOffsets(getBeginningOffsets(1));
        consumer.schedulePollTask(() -> {
            consumer.rebalance(Collections.singletonList(topicPartition));
            consumer.addRecord(record1);
            consumer.addRecord(record2);
        });

        Sink sink = getSink();
        consumer.schedulePollTask(() -> {
            OffsetAndMetadata committed = consumer.committed(Set.of(topicPartition)).get(topicPartition);
            assertEquals(record2.offset() + 1, committed.offset());
            sink.stop(10, TimeUnit.SECONDS);
        });
        sink.run();
    }

    private Sink getSink() {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        Properties props = new Properties();
        props.setProperty(SinkProps.BATCH_SIZE.name(), "2");
        return new Sink(
                executorService,
                "daemonId",
                props,
                parallelSender,
                Subscription.builder().build(),
                EventDeserializer.parseAllTags(),
                new SinkMetrics(new NoOpMetricsCollector())
        ) {
            @Override
            @NotNull Consumer<UUID, Event> getConsumer(
                    EventDeserializer deserializer,
                    Properties consumerProperties,
                    UuidDeserializer keyDeserializer
            ) {
                return consumer;
            }
        };
    }

    private static HashMap<TopicPartition, Long> getBeginningOffsets(int partitionsSize) {
        HashMap<TopicPartition, Long> startOffsets = new HashMap<>();
        for (int partition = 0; partition < partitionsSize; partition++) {
            TopicPartition tp = new TopicPartition(TestUtils.TOPIC, partition);
            startOffsets.put(tp, 0L);
        }
        return startOffsets;
    }
}
