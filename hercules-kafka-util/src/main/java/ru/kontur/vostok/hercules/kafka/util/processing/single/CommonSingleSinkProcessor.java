package ru.kontur.vostok.hercules.kafka.util.processing.single;

import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import org.apache.kafka.streams.processor.AbstractProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.kafka.util.processing.BackendServiceFailedException;
import ru.kontur.vostok.hercules.kafka.util.processing.SinkStatus;
import ru.kontur.vostok.hercules.kafka.util.processing.SinkStatusFsm;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.util.logging.LoggingConstants;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * CommonSingleSinkProcessor
 *
 * @author Kirill Sulim
 */
@Deprecated
public class CommonSingleSinkProcessor extends AbstractProcessor<UUID, Event> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommonSingleSinkProcessor.class);

    private static final Logger RECEIVED_EVENTS_LOGGER = LoggerFactory.getLogger(LoggingConstants.RECEIVED_EVENT_LOGGER_NAME);
    private static final Logger PROCESSED_EVENTS_LOGGER = LoggerFactory.getLogger(LoggingConstants.PROCESSED_EVENT_LOGGER_NAME);
    private static final Logger DROPPED_EVENTS_LOGGER = LoggerFactory.getLogger(LoggingConstants.DROPPED_EVENT_LOGGER_NAME);


    private final SinkStatusFsm status;
    private final SingleSender<UUID, Event> sender;

    private final Meter receivedEventsMeter;
    private final Meter receivedEventsSizeMeter;
    private final Meter processedEventsMeter;
    private final Meter droppedEventsMeter;
    private final Timer processTimeTimer;

    public CommonSingleSinkProcessor(
            SinkStatusFsm status,
            SingleSender<UUID, Event> sender,
            Meter receivedEventsMeter,
            Meter receivedEventsSizeMeter,
            Meter processedEventsMeter,
            Meter droppedEventsMeter,
            Timer processTimeTimer
    ) {
        this.status = status;
        this.sender = sender;

        this.receivedEventsMeter = receivedEventsMeter;
        this.receivedEventsSizeMeter = receivedEventsSizeMeter;
        this.processedEventsMeter = processedEventsMeter;
        this.droppedEventsMeter = droppedEventsMeter;
        this.processTimeTimer = processTimeTimer;
    }

    @Override
    public void process(UUID key, Event value) {
        markReceivedEvent(value);
        boolean dataSent = false;
        do {
            try {
                status.waitForState(
                        SinkStatus.RUNNING,
                        SinkStatus.STOPPING_FROM_INIT,
                        SinkStatus.STOPPING_FROM_RUNNING,
                        SinkStatus.STOPPING_FROM_SUSPEND
                );
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException("Should never happened", e);
            }
            if (!status.isRunning()) {
                return;
            }
            try {
                long start = System.currentTimeMillis();
                boolean processed = sender.process(key, value);
                if (processed) {
                    markProcessedEvent(value);
                } else {
                    markDroppedEvent(value);
                }
                long elapsed = System.currentTimeMillis() - start;
                processTimeTimer.update(elapsed, TimeUnit.MILLISECONDS);
                dataSent = true;
            } catch (BackendServiceFailedException e) {
                status.markBackendFailed();
            }
        } while (!dataSent);
    }

    private void markReceivedEvent(Event event) {
        if (RECEIVED_EVENTS_LOGGER.isTraceEnabled()) {
            RECEIVED_EVENTS_LOGGER.trace("{}", event.getUuid());
        }
        receivedEventsMeter.mark();
        receivedEventsSizeMeter.mark(event.getBytes().length);
    }

    private void markDroppedEvent(Event event) {
        if (DROPPED_EVENTS_LOGGER.isTraceEnabled()) {
            DROPPED_EVENTS_LOGGER.trace("{}", event.getUuid());
        }
        droppedEventsMeter.mark();
    }

    private void markProcessedEvent(Event event) {
        if (PROCESSED_EVENTS_LOGGER.isTraceEnabled()) {
            PROCESSED_EVENTS_LOGGER.trace("{}", event.getUuid());
        }
        processedEventsMeter.mark();
    }
}
