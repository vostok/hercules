package ru.kontur.vostok.hercules.elasticsearch.sink;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class BulkProcessor<T> {

    private final List<T> accumulator;
    private final int bulkSize;
    private final Consumer<List<T>> consumer;

    public BulkProcessor(Consumer<List<T>> consumer, int bulkSize) {
        this.accumulator = new ArrayList<>(bulkSize);
        this.bulkSize = bulkSize;
        this.consumer = consumer;
    }

    public void add(T t) {
        accumulator.add(t);
        if (accumulator.size() >= bulkSize) {
            consumer.accept(accumulator);
            accumulator.clear();
        }
    }
}
