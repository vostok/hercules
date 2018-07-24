package ru.kontur.vostok.hercules.gateway.client;

import ru.kontur.vostok.hercules.gateway.client.util.EventWriterUtil;
import ru.kontur.vostok.hercules.protocol.CommonConstants;
import ru.kontur.vostok.hercules.protocol.Event;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * @author Daniil Zhenikhov
 */
public class EventPublisher {
    private final Object monitor = new Object();
    private final Map<String, EventQueue> queueMap = new HashMap<>();
    private final GatewayClient gatewayClient = new GatewayClient();

    private final ScheduledThreadPoolExecutor executor;
    private final boolean loseOnOverflow;
    private final String url;
    private final String apiKey;

    /**
     * @param threads        count of worker-threads
     * @param loseOnOverflow flag is it possible to lose events when the queue is full
     * @param threadFactory  factory for worker-threads
     * @param url            url where events should be sent
     * @param apiKey         api key for sending events
     */
    public EventPublisher(int threads,
                          boolean loseOnOverflow,
                          ThreadFactory threadFactory,
                          List<EventQueue> queues,
                          String url,
                          String apiKey) {
        registerAll(queues);

        this.url = url;
        this.apiKey = apiKey;
        this.executor = new ScheduledThreadPoolExecutor(threads, threadFactory);
        this.loseOnOverflow = loseOnOverflow;
    }

    public EventPublisher(int threads,
                          boolean loseOnOverflow,
                          List<EventQueue> queues,
                          String url,
                          String apiKey) {
        this(threads, loseOnOverflow, Executors.defaultThreadFactory(), queues, url, apiKey);
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

    public void register(String name, String stream, long periodMillis, int capacity, int batchSize) {
        register(new EventQueue(name, stream, periodMillis, capacity, batchSize));
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
            if (loseOnOverflow) {
                return;
            }

            try {
                eventQueue.put(event);
            } catch (InterruptedException e1) {
                e1.printStackTrace();
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

        gatewayClient.close();
    }

    /**
     * Forms a batch of events and sends them to the {@link #url}
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
        int nextUnprocessedIndex = 0;

        for (int index = 0; index < events.size(); index++) {
            if (size + events.get(index).getBytes().length >= CommonConstants.MAX_MESSAGE_SIZE) {
                sendSliceEvents(events, eventQueue.getStream(), size, nextUnprocessedIndex, index);

                size = 0;
                nextUnprocessedIndex = index;
            }

            size += events.get(index).getBytes().length;
        }

        sendSliceEvents(events, eventQueue.getStream(), size, nextUnprocessedIndex, events.size());

        return actualBatchSize;
    }

    /**
     * Create array from subList of <code>events</code> and send its to stream
     *
     * @param events source events list
     * @param stream topic name in kafka where should be send data
     * @param size total size events in list
     * @param startSlice start of sublist
     * @param endSlice end of sublist
     */
    private void sendSliceEvents(List<Event> events,
                                 String stream,
                                 int size,
                                 int startSlice,
                                 int endSlice) {
        Event[] eventsArray = events
                .subList(startSlice, endSlice)
                .toArray(new Event[endSlice - startSlice]);

        gatewayClient.sendAsync(
                this.url,
                this.apiKey,
                stream,
                EventWriterUtil.toBytes(size, eventsArray));

    }

    private void startQueueWorker(EventQueue eventQueue) {
        executor.scheduleAtFixedRate(
                () -> process(eventQueue),
                eventQueue.getPeriodMillis(),
                eventQueue.getPeriodMillis(),
                TimeUnit.MILLISECONDS
        );
    }
}
