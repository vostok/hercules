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

    public void start() {
        executor.scheduleAtFixedRate(
                this::process,
                periodMillis,
                periodMillis,
                TimeUnit.MILLISECONDS
        );
    }

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

    public void stop(long timeoutMillis) {
        executor.shutdown();

        if (timeoutMillis > 0) {
            long nanos = System.nanoTime();
            while (TimeUnit.MILLISECONDS.toNanos(timeoutMillis) > System.nanoTime() - nanos && process() > 0) {
            }
        }

        gatewayClient.close();
    }

    private int process() {
        List<Event> events = new ArrayList<>(batchSize);
        int actualBatchSize = queue.drainTo(events, batchSize);

        if (actualBatchSize == 0) {
            return 0;
        }

        Event[] eventsArray = new Event[actualBatchSize];
        eventsArray = events.toArray(eventsArray);

        gatewayClient.sendAsync(this.url, this.apiKey, this.stream, EventWriterUtil.toBytes(CommonConstants.MAX_MESSAGE_SIZE, eventsArray));

        return actualBatchSize;
    }
}
