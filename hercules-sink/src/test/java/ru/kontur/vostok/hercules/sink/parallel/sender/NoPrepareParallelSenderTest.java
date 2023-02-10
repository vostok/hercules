package ru.kontur.vostok.hercules.sink.parallel.sender;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.kontur.vostok.hercules.health.NoOpMetricsCollector;
import ru.kontur.vostok.hercules.kafka.util.processing.BackendServiceFailedException;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.sink.ProcessorResult;
import ru.kontur.vostok.hercules.sink.parallel.TestUtils;
import ru.kontur.vostok.hercules.sink.parallel.sender.NoPrepareParallelSender;

import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class NoPrepareParallelSenderTest {
    NoPrepareParallelSender okNoPrepareParallelSender;
    NoPrepareParallelSender errorNoPrepareParallelSender;

    @BeforeEach
    void setUp() {
        okNoPrepareParallelSender = new NoPrepareParallelSender(new Properties(), new NoOpMetricsCollector()) {
            @Override
            protected int send(List<Event> events) {
                return 1;
            }
        };

        errorNoPrepareParallelSender = new NoPrepareParallelSender(new Properties(), new NoOpMetricsCollector()) {
            @Override
            protected int send(List<Event> events) throws BackendServiceFailedException {
                throw new BackendServiceFailedException();
            }
        };
    }

    @Test
    void prepare() {
        NoPrepareParallelSender.NoPrepareEvents prepare = okNoPrepareParallelSender.prepare(List.of(TestUtils.createEvent()));
        assertEquals(1, prepare.getEventsCount());
    }

    @Test
    void processSend() {
        NoPrepareParallelSender.NoPrepareEvents noPrepareEvents = new NoPrepareParallelSender.NoPrepareEvents(List.of(TestUtils.createEvent()));
        ProcessorResult processorResult = okNoPrepareParallelSender.processSend(noPrepareEvents);
        assertEquals(1, processorResult.getProcessedEvents());
        assertTrue(okNoPrepareParallelSender.isAvailable());
    }

    @Test
    void errorPrepare() {
        NoPrepareParallelSender.NoPrepareEvents prepareEvents = errorNoPrepareParallelSender.prepare(List.of(TestUtils.createEvent()));
        assertEquals(1, prepareEvents.getEventsCount());
    }

    @Test
    void errorProcessSend() {
        NoPrepareParallelSender.NoPrepareEvents noPrepareEvents = new NoPrepareParallelSender.NoPrepareEvents(List.of(TestUtils.createEvent()));
        ProcessorResult processorResult = errorNoPrepareParallelSender.processSend(noPrepareEvents);
        assertFalse(processorResult.isSuccess());
        assertEquals(0, processorResult.getProcessedEvents());
        assertFalse(errorNoPrepareParallelSender.isAvailable());
    }
}