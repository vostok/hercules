package ru.kontur.vostok.hercules.kafka.util.processing;

import org.apache.kafka.streams.processor.AbstractProcessor;
import org.apache.kafka.streams.processor.Cancellable;
import org.apache.kafka.streams.processor.PunctuationType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;

public class BulkProcessor<K, V> extends AbstractProcessor<K, V> {

    private final int punctuationInterval;
    private final LinkedBlockingQueue<Entry<K, V>> queue;
    private final BulkSender<K, V> sender;

    // Cancellation doesn't work. Probably because of https://issues.apache.org/jira/browse/KAFKA-6748
    // TODO: check that cancellation works correctly after kafka 1.1.1 release
    private final AtomicReference<Cancellable> scheduled = new AtomicReference<>(null);

    public BulkProcessor(BulkSender<K, V> sender, int batchSize, int punctuationInterval) {
        this.punctuationInterval = punctuationInterval;
        this.queue = new LinkedBlockingQueue<>(batchSize);
        this.sender = sender;
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
            sender.send(elementsToProcess);
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

    @Override
    public void close() {
        super.close();
        cancelProcessing();
    }
}
