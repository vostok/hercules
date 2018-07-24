package ru.kontur.vostok.hercules.gateway.client;

import ru.kontur.vostok.hercules.protocol.Event;

import java.util.Collection;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Daniil Zhenikhov
 */
public class EventQueue {
    private final BlockingQueue<Event> blockingQueue;
    private final String name;
    private final String stream;
    private final long periodMillis;
    private final int batchSize;

    private final AtomicInteger currentBytesSize = new AtomicInteger();


    public EventQueue(String name, String stream, long periodMillis, int capacity, int batchSize) {
        this.name = name;
        this.periodMillis = periodMillis;
        this.stream = stream;
        this.batchSize = batchSize;

        this.blockingQueue = new ArrayBlockingQueue<>(capacity);
    }

    public int drainTo(Collection<Event> collection, int maxElements) {
        int actual = blockingQueue.drainTo(collection, maxElements);

        collection.forEach(event -> currentBytesSize.addAndGet(-event.getBytes().length));

        return actual;
    }

    public int eventsBytesSize() {
        return currentBytesSize.get();
    }

    public int size() {
        return blockingQueue.size();
    }

    public void add(Event event) {
        blockingQueue.add(event);
        currentBytesSize.addAndGet(event.getBytes().length);
    }

    public void put(Event event) throws InterruptedException {
        blockingQueue.put(event);
        currentBytesSize.addAndGet(event.getBytes().length);
    }

    public String getName() {
        return name;
    }

    public long getPeriodMillis() {
        return periodMillis;
    }

    public String getStream() {
        return stream;
    }

    public int getBatchSize() {
        return batchSize;
    }
}
