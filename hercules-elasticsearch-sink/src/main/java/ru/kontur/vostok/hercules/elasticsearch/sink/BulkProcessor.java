package ru.kontur.vostok.hercules.elasticsearch.sink;

import org.apache.kafka.streams.processor.AbstractProcessor;
import org.apache.kafka.streams.processor.Cancellable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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

    private final LinkedBlockingQueue<Entry<K, V>> queue;
    private final Consumer<Collection<Entry<K, V>>> bulkProcessor;

    public BulkProcessor(Consumer<Collection<Entry<K, V>>> bulkProcessor, int batchSize) {
        this.queue = new LinkedBlockingQueue<>(batchSize);
        this.bulkProcessor = bulkProcessor;
    }

    @Override
    public void process(K key, V value) {
        queue.add(new Entry<>(key, value));
        if (queue.remainingCapacity() == 0) {
            processQueue();
        }
    }

    private void processQueue() {
        int size = queue.size();
        List<Entry<K, V>> elementsToProcess = new ArrayList<>(size);
        queue.drainTo(elementsToProcess, size);
        bulkProcessor.accept(elementsToProcess);

        // FIXME: Add scheduling
    }
}
