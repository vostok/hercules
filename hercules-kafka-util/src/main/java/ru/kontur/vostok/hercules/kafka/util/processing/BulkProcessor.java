package ru.kontur.vostok.hercules.kafka.util.processing;

import org.apache.kafka.streams.processor.AbstractProcessor;
import org.apache.kafka.streams.processor.Cancellable;
import org.apache.kafka.streams.processor.PunctuationType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class BulkProcessor<K, V> extends AbstractProcessor<K, V> {

    public static class Entry<K, V> {
        private final K key;
        private final V value;

        public Entry(K key, V value) {
            this.key = key;
            this.value = value;
        }

        public K getKey() {
            return key;
        }

        public V getValue() {
            return value;
        }
    }

    private final int punctuationInterval;
    private final LinkedBlockingQueue<Entry<K, V>> queue;
    private final Consumer<Collection<Entry<K, V>>> bulkProcessor;
    private final AtomicReference<Cancellable> scheduled = new AtomicReference<>(null);

    public BulkProcessor(Consumer<Collection<Entry<K, V>>> bulkProcessor, int batchSize, int punctuationInterval) {
        this.punctuationInterval = punctuationInterval;
        this.queue = new LinkedBlockingQueue<>(batchSize);
        this.bulkProcessor = bulkProcessor;
    }

    @Override
    public void process(K key, V value) {
        queue.add(new Entry<>(key, value));
        if (queue.remainingCapacity() == 0) {
            processQueue();
        } else {
            scheduleProcessing();
        }
    }

    private void processQueue() {
        int size = queue.size();
        if (0 != size) {
            List<Entry<K, V>> elementsToProcess = new ArrayList<>(size);
            queue.drainTo(elementsToProcess, size);
            bulkProcessor.accept(elementsToProcess);
            context().commit();
        }

        cancelProcessing();
        if (0 != queue.size()) {
            scheduleProcessing();
        }
    }

    private void scheduleProcessing() {
        if (Objects.isNull(scheduled.get())) {
            Cancellable newProcessing = context().schedule(punctuationInterval, PunctuationType.WALL_CLOCK_TIME, ts -> processQueue());
            if (!scheduled.compareAndSet(null, newProcessing)) {
                newProcessing.cancel();
            }
        }
    }

    private void cancelProcessing() {
        Cancellable ref = scheduled.get();
        if (Objects.nonNull(ref)) {
            if (scheduled.compareAndSet(ref, null)) {
                ref.cancel();
            }
        }
    }
}
