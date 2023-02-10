package ru.kontur.vostok.hercules.sink.parallel;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.MockConsumer;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.kontur.vostok.hercules.application.Application;
import ru.kontur.vostok.hercules.application.ApplicationRunner;
import ru.kontur.vostok.hercules.health.NoOpMetricsCollector;
import ru.kontur.vostok.hercules.sink.parallel.sender.ParallelSender;

import java.util.Properties;

@ExtendWith(MockitoExtension.class)
public class ParallelSinkDaemonStarterTest {

    @Mock
    ParallelSender<TestPreparedData> parallelSender;

    @Mock
    ApplicationRunner applicationRunner;

    Application app;

    @BeforeEach
    public void setUp() {
        app = Application.run(applicationRunner, "application.properties=resource://base.properties");
    }

    @AfterEach
    public void afterAll() {
        app.stop();
    }

    @Test
    void start() {
        ParallelSinkDaemonStarter<TestPreparedData> sinkDaemonStarter = getSinkDaemonStarter();
        sinkDaemonStarter.start(app.getConfig().getAllProperties(), app.getContainer());
    }

    private ParallelSinkDaemonStarter<TestPreparedData> getSinkDaemonStarter() {
        return new ParallelSinkDaemonStarter<>(
                parallelSender,
                new NoOpEventsBatchListener<>(),
                "daemonId",
                new NoOpMetricsCollector()
        ) {
            Consumer<byte[], byte[]> getConsumer(Properties consumerProperties) {
                return new MockConsumer<>(OffsetResetStrategy.EARLIEST);
            }
        };
    }
}