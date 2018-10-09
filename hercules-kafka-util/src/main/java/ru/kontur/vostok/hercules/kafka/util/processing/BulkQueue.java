package ru.kontur.vostok.hercules.kafka.util.processing;

import ru.kontur.vostok.hercules.util.functional.Result;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

/**
 * BulkQueue
 *
 * @author Kirill Sulim
 */
public class BulkQueue<Key, Value> {

    public static class RunResult<Key, Value> {
        private final RecordStorage<Key, Value> storage;
        private final BulkSenderStat stat;

        public RunResult(
                RecordStorage<Key, Value> storage,
                BulkSenderStat stat
        ) {
            this.storage = storage;
            this.stat = stat;
        }

        public RecordStorage<Key, Value> getStorage() {
            return storage;
        }

        public BulkSenderStat getStat() {
            return stat;
        }
    }

    public static class RunUnit<Key, Value> {
        private final RecordStorage<Key, Value> storage;
        private final CompletableFuture<Result<BulkQueue.RunResult<Key, Value>, BackendServiceFailedException>> future;

        public RunUnit(
                RecordStorage<Key, Value> storage,
                CompletableFuture<Result<BulkQueue.RunResult<Key, Value>, BackendServiceFailedException>> future
        ) {
            this.storage = storage;
            this.future = future;
        }

        public RecordStorage<Key, Value> getStorage() {
            return storage;
        }

        public CompletableFuture<Result<RunResult<Key, Value>, BackendServiceFailedException>> getFuture() {
            return future;
        }
    }

    private final BlockingQueue<BulkQueue.RunUnit<Key, Value>> queue;

    public BulkQueue(int queueSize) {
        this.queue = new ArrayBlockingQueue<>(queueSize);
    }

    public Future<Result<BulkQueue.RunResult<Key, Value>, BackendServiceFailedException>> put(RecordStorage<Key, Value> storage) throws InterruptedException {
        CompletableFuture<Result<BulkQueue.RunResult<Key, Value>, BackendServiceFailedException>> future = new CompletableFuture<>();
        queue.put(new BulkQueue.RunUnit<>(storage, future));
        return future;
    }

    public RunUnit<Key, Value> take() throws InterruptedException {
         return queue.take();
    }
}
