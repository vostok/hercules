package ru.kontur.vostok.hercules.sink;

import org.junit.jupiter.api.Test;
import ru.kontur.vostok.hercules.health.NoOpMetricsCollector;
import ru.kontur.vostok.hercules.kafka.util.processing.BackendServiceFailedException;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.sink.parallel.sender.NoPrepareParallelSender;
import ru.kontur.vostok.hercules.sink.parallel.TestUtils;

import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class NoPrepareParallelSenderAsSenderTest {

    @Test
    void send() throws BackendServiceFailedException {
        assertEquals(1, okNoPrepareParallelSender.send(List.of(TestUtils.createEvent())));
    }

    @Test
    void errorSend() {
        assertThrows(BackendServiceFailedException.class, () -> {
            errorNoPrepareParallelSender.send(List.of(TestUtils.createEvent()));
        });
    }

    NoPrepareParallelSender okNoPrepareParallelSender = new NoPrepareParallelSender(new Properties(), new NoOpMetricsCollector()) {
        @Override
        protected int send(List<Event> events) {
            return 1;
        }
    };

    NoPrepareParallelSender errorNoPrepareParallelSender = new NoPrepareParallelSender(new Properties(), new NoOpMetricsCollector()) {
        @Override
        protected int send(List<Event> events) throws BackendServiceFailedException {
            throw new BackendServiceFailedException();
        }
    };
}