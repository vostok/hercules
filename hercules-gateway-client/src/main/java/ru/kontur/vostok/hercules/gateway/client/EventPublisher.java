package ru.kontur.vostok.hercules.gateway.client;

import ru.kontur.vostok.hercules.gateway.client.util.EventWriterUtil;
import ru.kontur.vostok.hercules.protocol.CommonConstants;
import ru.kontur.vostok.hercules.protocol.Event;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * @author Daniil Zhenikhov
 */
public class EventPublisher {
    private final Object monitor = new Object();
    private final String stream;
    private final long periodMillis;
    private final int batchSize;
    private final ScheduledThreadPoolExecutor executor;
    private final ArrayBlockingQueue<Event> queue;
    private final boolean loseOnOverflow;
    private final GatewayClient gatewayClient;
    private final String url;
    private final String apiKey;

    /**
     * @param stream topic in kafka
     * @param periodMillis period for processing events by fixed rate scheduler. In millis
     * @param batchSize size of events' batch
     * @param threads count of worker-threads
     * @param capacity initial capacity for events' queue
     * @param loseOnOverflow flag is it possible to lose events when the queue is full
     * @param threadFactory factory for worker-threads
     * @param url url where events should be sent
     * @param apiKey api key for sending events
     */
    public EventPublisher(String stream,
                          long periodMillis,
                          int batchSize,
                          int threads,
                          int capacity,
                          boolean loseOnOverflow,
                          ThreadFactory threadFactory,
                          String url,
                          String apiKey) {
        this.url = url;
        this.apiKey = apiKey;
        this.stream = stream;
        this.periodMillis = periodMillis;
        this.batchSize = batchSize;
        this.executor = new ScheduledThreadPoolExecutor(threads, threadFactory);
        this.loseOnOverflow = loseOnOverflow;

        this.queue = new ArrayBlockingQueue<>(capacity);
        this.gatewayClient = new GatewayClient();
    }

    /**
     * Start publisher and schedule {@link #process()} queue in fixed rate <code>periodMillis</code>
     */
    public void start() {
        executor.scheduleAtFixedRate(
                this::process,
                periodMillis,
                periodMillis,
                TimeUnit.MILLISECONDS
        );
    }

    /**
     * Publish event and schedule {@link #process()} if queue has batch of events
     * @param event event for publishing
     */
    public void publish(Event event) {
        try {
            queue.add(event);

            int currentSize = queue.size();
            if (currentSize < batchSize) {
                return;
            }

            if (batchSize * (executor.getQueue().size() + 1) < currentSize) {
                synchronized (monitor) {
                    if (batchSize * (executor.getQueue().size() + 1) < queue.size()) {
                        executor.schedule(this::process, 0, TimeUnit.MILLISECONDS);
                    }
                }
            }
        } catch (IllegalStateException e) {
            if (loseOnOverflow) {
                return;
            }

            try {
                queue.put(event);
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
            long nanos = System.nanoTime();
            while (TimeUnit.MILLISECONDS.toNanos(timeoutMillis) > System.nanoTime() - nanos && process() > 0) {
                /* Empty */
            }
        }

        gatewayClient.close();
    }

    /**
     * Forms a batch of events and sends them to the {@link #url}
     *
     * @return actual count of events which has been processed
     */
    private int process() {
        List<Event> events = new ArrayList<>(batchSize);
        int actualBatchSize = queue.drainTo(events, batchSize);

        if (actualBatchSize == 0) {
            return 0;
        }

        Event[] eventsArray = new Event[actualBatchSize];
        eventsArray = events.toArray(eventsArray);

        gatewayClient.sendAsync(this.url, this.apiKey, this.stream, EventWriterUtil.toBytes(eventsArray));

        return actualBatchSize;
    }
}
