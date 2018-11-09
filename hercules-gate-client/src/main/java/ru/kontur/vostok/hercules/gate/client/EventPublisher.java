package ru.kontur.vostok.hercules.gate.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.configuration.Scopes;
import ru.kontur.vostok.hercules.configuration.util.PropertiesUtil;
import ru.kontur.vostok.hercules.gate.client.exception.BadRequestException;
import ru.kontur.vostok.hercules.gate.client.exception.UnavailableClusterException;
import ru.kontur.vostok.hercules.gate.client.util.EventWriterUtil;
import ru.kontur.vostok.hercules.protocol.CommonConstants;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.util.properties.PropertyDescription;
import ru.kontur.vostok.hercules.util.properties.PropertyDescriptions;
import ru.kontur.vostok.hercules.util.validation.ArrayValidators;
import ru.kontur.vostok.hercules.util.validation.IntegerValidators;
import ru.kontur.vostok.hercules.util.validation.Validators;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * @author Daniil Zhenikhov
 */
public class EventPublisher {
    private static final Logger LOGGER = LoggerFactory.getLogger(EventPublisher.class);

    private final Object monitor = new Object();
    private final Map<String, EventQueue> queueMap = new HashMap<>();
    private final GateClient gateClient;

    private final ScheduledThreadPoolExecutor executor;
    private final String[] urls;
    private final String apiKey;

    /**
     * Note that <code>threadFactory</code> should create daemon-thread. It's needing for correct stopping.
     *
     * @param properties    configuration properties
     * @param threadFactory factory for worker-threads
     * @param queues        event queues
     */
    public EventPublisher(Properties properties,
                          ThreadFactory threadFactory,
                          List<EventQueue> queues) {
        final int threads = Props.THREAD_COUNT.extract(properties);
        final String[] urls = Props.URLS.extract(properties);
        final String apiKey = Props.API_KEY.extract(properties);
        final Properties gateClientProperties = PropertiesUtil.ofScope(properties, Scopes.GATE_CLIENT);

        this.urls = urls;
        this.apiKey = apiKey;
        this.executor = new ScheduledThreadPoolExecutor(threads, threadFactory);

        this.gateClient = new GateClient(gateClientProperties);

        registerAll(queues);


    }

    public void start() {
        for (Map.Entry<String, EventQueue> entry : queueMap.entrySet()) {
            startQueueWorker(entry.getValue());
        }
    }

    public void register(EventQueue eventQueue) {
        queueMap.put(eventQueue.getName(), eventQueue);
        startQueueWorker(eventQueue);
    }

    public void register(String name,
                         String stream,
                         long periodMillis,
                         int capacity,
                         int batchSize,
                         boolean loseOnOverflow) {
        register(new EventQueue(name, stream, periodMillis, capacity, batchSize, loseOnOverflow));
    }

    public void registerAll(Collection<EventQueue> eventQueues) {
        for (EventQueue eventQueue : eventQueues) {
            register(eventQueue);
        }
    }

    /**
     * Publish event and schedule {@link #process(EventQueue)} if queue has batch of events
     *
     * @param queueName name of queue where event will be published
     * @param event     event for publishing
     */
    public void publish(String queueName, Event event) {
        if (!queueMap.containsKey(queueName)) {
            throw new IllegalArgumentException("Event queue with '" + queueName + "' name does not exist");
        }

        EventQueue eventQueue = queueMap.get(queueName);
        int batchSize = eventQueue.getBatchSize();

        try {
            eventQueue.add(event);

            int currentCount = eventQueue.size();
            if (currentCount < batchSize) {
                return;
            }

            if (batchSize * (executor.getQueue().size() + 1) < currentCount) {
                synchronized (monitor) {
                    if (batchSize * (executor.getQueue().size() + 1) < eventQueue.size()) {
                        executor.schedule(() -> process(eventQueue), 0, TimeUnit.MILLISECONDS);
                    }
                }
            }
        } catch (IllegalStateException e) {
            if (eventQueue.isLoseOnOverflow()) {
                return;
            }

            try {
                eventQueue.put(event);
            } catch (InterruptedException e1) {
                LOGGER.error("Interruption", e1);
            }
        }
    }

    /**
     * Stop executing of event publisher. Waits <code>timeoutMillis</code> milliseconds to send a portion of unhandled events
     *
     * @param timeoutMillis milliseconds for waiting before event publisher stop
     */
    public void stop(long timeoutMillis) {
        executor.shutdown();

        if (timeoutMillis > 0) {
            for (EventQueue eventQueue : queueMap.values()) {
                long nanos = System.nanoTime();
                while (TimeUnit.MILLISECONDS.toNanos(timeoutMillis) > System.nanoTime() - nanos && process(eventQueue) > 0) {
                    /* Empty */
                }
            }
        }

        gateClient.close();
    }

    /**
     * Forms a batch of events and sends them to the {@link #urls}
     *
     * @param eventQueue EventQueue should be processing
     * @return actual count of events which has been processed
     */
    private int process(EventQueue eventQueue) {
        List<Event> events = new ArrayList<>(eventQueue.getBatchSize());
        int actualBatchSize = eventQueue.drainTo(events, eventQueue.getBatchSize());

        if (actualBatchSize == 0) {
            return 0;
        }

        int size = 0;
        int lastUnprocessedIndex = 0;

        for (int index = 0; index < events.size(); index++) {
            if (events.get(index).getBytes().length >= CommonConstants.MAX_MESSAGE_SIZE)
                continue;

            if (size + events.get(index).getBytes().length >= CommonConstants.MAX_MESSAGE_SIZE) {
                sendSliceEvents(events, eventQueue.getStream(), size, lastUnprocessedIndex, index);

                size = 0;
                lastUnprocessedIndex = index;
            }

            size += events.get(index).getBytes().length;
        }

        sendSliceEvents(events, eventQueue.getStream(), size, lastUnprocessedIndex, events.size());

        return actualBatchSize;
    }

    /**
     * Create array from subList of <code>events</code> and send its to stream
     *
     * @param events     source events list
     * @param stream     topic name in kafka where should be send data
     * @param size       total size events in list
     * @param startSlice start of sublist
     * @param endSlice   end of sublist
     */
    private void sendSliceEvents(List<Event> events,
                                 String stream,
                                 int size,
                                 int startSlice,
                                 int endSlice) {
        Event[] eventsArray = events
                .subList(startSlice, endSlice)
                .toArray(new Event[endSlice - startSlice]);

        try {
            gateClient.sendAsync(
                    this.urls,
                    this.apiKey,
                    stream,
                    EventWriterUtil.toBytes(size, eventsArray));
        } catch (BadRequestException ignored) {
            LOGGER.warn("Failed to send a packet of events");
        } catch (UnavailableClusterException e) {
            LOGGER.warn("No url from cluster is available. Cluster = " + Arrays.toString(this.urls));
            throw new RuntimeException(e);
        }
    }

    private void startQueueWorker(EventQueue eventQueue) {
        executor.scheduleAtFixedRate(
                () -> process(eventQueue),
                eventQueue.getPeriodMillis(),
                eventQueue.getPeriodMillis(),
                TimeUnit.MILLISECONDS
        );
    }

    private static class Props {
        static final PropertyDescription<Integer> THREAD_COUNT =
                PropertyDescriptions
                        .integerProperty("threads")
                        .withDefaultValue(3)
                        .withValidator(IntegerValidators.positive())
                        .build();

        static final PropertyDescription<String[]> URLS =
                PropertyDescriptions
                        .arrayOfStringsProperty("urls")
                        .withValidator(ArrayValidators.notEmpty())
                        .build();

        static final PropertyDescription<String> API_KEY =
                PropertyDescriptions
                        .stringProperty("apiKey")
                        .build();
    }
}
