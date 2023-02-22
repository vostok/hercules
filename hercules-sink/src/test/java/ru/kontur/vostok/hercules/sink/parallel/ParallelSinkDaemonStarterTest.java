package ru.kontur.vostok.hercules.sink.parallel;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.MockConsumer;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.kontur.vostok.hercules.application.Application;
import ru.kontur.vostok.hercules.application.ApplicationRunner;
import ru.kontur.vostok.hercules.health.NoOpMetricsCollector;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.util.time.TimeSource;

import java.util.Collections;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.kontur.vostok.hercules.sink.parallel.TestUtils.TOPIC;
import static ru.kontur.vostok.hercules.sink.parallel.TestUtils.createEvent;
import static ru.kontur.vostok.hercules.sink.parallel.TestUtils.record;

@ExtendWith(MockitoExtension.class)
public class ParallelSinkDaemonStarterTest {

    @Mock
    ApplicationRunner applicationRunner;

    Application app;

    MockConsumer<byte[], byte[]> consumer;

    TestParallelSender parallelSender;

    @BeforeEach
    public void setUp() {
        parallelSender = new TestParallelSender();
        consumer = new MockConsumer<>(OffsetResetStrategy.EARLIEST);
    }

    @AfterEach
    public void afterAll() {
        app.stop();
    }

    @Test
    void switcherEventFilter_true() {
        app = Application.run(applicationRunner,
                "application.properties=resource://base.properties",
                "sink.filter.0.class=ru.kontur.vostok.hercules.sink.filter.SwitcherEventFilter",
                "sink.filter.0.props.on=true"
        );

        startAndWaitProcessEvent();
        assertEquals(1, parallelSender.lastPreparedData);
    }

    @Test
    void switcherEventFilter_false() {
        app = Application.run(applicationRunner,
                "application.properties=resource://base.properties",
                "sink.filter.0.class=ru.kontur.vostok.hercules.sink.filter.SwitcherEventFilter",
                "sink.filter.0.props.on=false"
        );

        startAndWaitProcessEvent();
        assertEquals(0, parallelSender.lastPreparedData);
    }

    private void startAndWaitProcessEvent() {
        TopicPartition topicPartition = new TopicPartition(TOPIC, 0);
        Event event = createEvent();
        consumer.updateBeginningOffsets(TestUtils.getBeginningOffsets(1));
        consumer.schedulePollTask(() -> {
            consumer.rebalance(Collections.singletonList(topicPartition));
            consumer.addRecord(record(topicPartition, event));
        });

        ParallelSinkDaemonStarter<TestPreparedData> sinkDaemonStarter = getSinkDaemonStarter();
        sinkDaemonStarter.start(app.getConfig().getAllProperties(), app.getContainer());

        while (parallelSender.lastPreparedData == Integer.MAX_VALUE) {
            TimeSource.SYSTEM.sleep(10);
        }
    }

    private ParallelSinkDaemonStarter<TestPreparedData> getSinkDaemonStarter() {
        return new ParallelSinkDaemonStarter<>(
                parallelSender,
                new NoOpEventsBatchListener<>(),
                "daemonId",
                new NoOpMetricsCollector()
        ) {
            Consumer<byte[], byte[]> getConsumer(Properties consumerProperties) {
                return consumer;
            }
        };
    }
}