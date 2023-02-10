package ru.kontur.vostok.hercules.sink.parallel.sender;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.kontur.vostok.hercules.health.NoOpMetricsCollector;
import ru.kontur.vostok.hercules.kafka.util.processing.BackendServiceFailedException;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.sink.ProcessorResult;
import ru.kontur.vostok.hercules.sink.parallel.TestPreparedData;
import ru.kontur.vostok.hercules.sink.parallel.TestUtils;

import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ParallelSenderTest {
    ParallelSender<TestPreparedData> okParallelSender;
    ParallelSender<TestPreparedData> errorParallelSender;
    ParallelSender<TestPreparedData> errorSendParallelSender;

    @BeforeEach
    void setUp() {
        okParallelSender = new ParallelSender<>(new Properties(), new NoOpMetricsCollector()) {

            @Override
            public TestPreparedData prepare(List<Event> events) {
                return new TestPreparedData(1);
            }

            @Override
            public int send(TestPreparedData preparedData) {
                return 1;
            }
        };
        errorParallelSender = new ParallelSender<>(new Properties(), new NoOpMetricsCollector()) {

            @Override
            public TestPreparedData prepare(List<Event> events) {
                throw new RuntimeException();
            }

            @Override
            public int send(TestPreparedData preparedData) throws BackendServiceFailedException {
                throw new BackendServiceFailedException();
            }
        };
        errorSendParallelSender = new ParallelSender<>(new Properties(), new NoOpMetricsCollector()) {

            @Override
            public TestPreparedData prepare(List<Event> events) {
                return new TestPreparedData(1);
            }

            @Override
            public int send(TestPreparedData preparedData) throws BackendServiceFailedException {
                throw new BackendServiceFailedException();
            }
        };
    }

    @Test
    void send() throws BackendServiceFailedException {
        Assertions.assertEquals(1, okParallelSender.send(List.of(TestUtils.createEvent())));
    }

    @Test
    void prepare() {
        TestPreparedData prepare = okParallelSender.prepare(List.of(TestUtils.createEvent()));
        assertEquals(1, prepare.getEventsCount());
    }

    @Test
    void processSend() {
        ProcessorResult processorResult = okParallelSender.processSend(new TestPreparedData(1));
        assertEquals(1, processorResult.getProcessedEvents());
        assertTrue(okParallelSender.isAvailable());
    }

    @Test
    void errorSend() {
        assertThrows(RuntimeException.class, () -> {
            errorParallelSender.send(List.of(TestUtils.createEvent()));
        });
    }

    @Test
    void errorPrepare() {
        assertThrows(RuntimeException.class, () -> {
            errorParallelSender.prepare(List.of(TestUtils.createEvent()));
        });
    }

    @Test
    void errorProcessSend() {
        ProcessorResult processorResult = errorSendParallelSender.processSend(new TestPreparedData(1));
        assertFalse(processorResult.isSuccess());
        assertEquals(0, processorResult.getProcessedEvents());
        assertFalse(errorSendParallelSender.isAvailable());
    }

    @Test
    void errorSend_Send() {
        assertThrows(BackendServiceFailedException.class, () -> {
            errorSendParallelSender.send(List.of(TestUtils.createEvent()));
        });
    }

    @Test
    void errorSend_Prepare() {
        TestPreparedData prepare = errorSendParallelSender.prepare(List.of(TestUtils.createEvent()));
        assertEquals(1, prepare.getEventsCount());
    }

    @Test
    void errorSend_ProcessSend() {
        ProcessorResult processorResult = errorParallelSender.processSend(new TestPreparedData(1));
        assertFalse(processorResult.isSuccess());
        assertEquals(0, processorResult.getProcessedEvents());
    }
}