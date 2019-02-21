package ru.kontur.vostok.hercules.sink;

import com.codahale.metrics.Meter;
import org.apache.kafka.clients.consumer.CommitFailedException;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.util.logging.LoggingConstants;
import ru.kontur.vostok.hercules.util.properties.PropertyDescription;
import ru.kontur.vostok.hercules.util.properties.PropertyDescriptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

/**
 * @author Gregory Koshelev
 */
public class SimpleSink extends Sink {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleSink.class);
    private static final Logger DROPPED_EVENTS_LOGGER = LoggerFactory.getLogger(LoggingConstants.DROPPED_EVENT_LOGGER_NAME);


    private final Sender sender;

    private final Meter droppedEventsMeter;
    private final Meter processedEventsMeter;
    private final Meter rejectedEventsMeter;
    private final Meter totalEventsMeter;

    private final long senderTimeoutMs;

    public SimpleSink(
            ExecutorService executor,
            String applicationId,
            Properties properties,

            Sender sender, Meter droppedEventsMeter, Meter processedEventsMeter, Meter rejectedEventsMeter, Meter totalEventsMeter) {
        super(executor, applicationId, properties);

        this.sender = sender;

        this.droppedEventsMeter = droppedEventsMeter;
        this.processedEventsMeter = processedEventsMeter;
        this.rejectedEventsMeter = rejectedEventsMeter;
        this.totalEventsMeter = totalEventsMeter;

        this.senderTimeoutMs = Props.SENDER_TIMEOUT_MS.extract(properties);
    }

    @Override
    public void run() {
        while (isRunning()) {
            try {
                if (!sender.isAvailable()) {
                    return;
                }
                subscribe();

                while (sender.isAvailable()) {
                    ConsumerRecords<UUID, Event> pollResult;
                    try {
                        pollResult = poll();
                    } catch (WakeupException ex) {
                        /*
                         * WakeupException is used to terminate polling
                         */
                        return;
                    }

                    Set<TopicPartition> partitions = pollResult.partitions();

                    // ConsumerRecords::count works for O(n), where n is partition count
                    int eventCount = pollResult.count();
                    List<Event> events = new ArrayList<>(eventCount);

                    int droppedEvents = 0;

                    for (TopicPartition partition : partitions) {
                        List<ConsumerRecord<UUID, Event>> records = pollResult.records(partition);
                        for (ConsumerRecord<UUID, Event> record : records) {
                            Event event = record.value();
                            if (event == null) {// Received non-deserializable data, should be ignored
                                droppedEvents++;
                                if (DROPPED_EVENTS_LOGGER.isDebugEnabled()) {
                                    DROPPED_EVENTS_LOGGER.trace("{}", record.key());
                                }
                                continue;
                            }
                            events.add(event);
                        }
                    }

                    SenderResult result = sender.process(events);
                    if (result.isSuccess()) {
                        try {
                            commit();
                            droppedEventsMeter.mark(droppedEvents);
                            processedEventsMeter.mark(result.getProcessedEvents());
                            rejectedEventsMeter.mark(result.getRejectedEvents());
                            totalEventsMeter.mark(events.size());
                        } catch (CommitFailedException ex) {
                            LOGGER.warn("Commit failed due to rebalancing", ex);
                            continue;
                        }
                    }
                }
            } catch (Exception ex) {
                LOGGER.error("Unspecified exception has been acquired", ex);
            } finally {
                unsubscribe();
            }

            sender.awaitAvailability(senderTimeoutMs);
        }
    }

    private static class Props {
        static final PropertyDescription<Long> SENDER_TIMEOUT_MS =
                PropertyDescriptions.longProperty("senderTimeoutMs").withDefaultValue(2_000L).build();
    }
}
