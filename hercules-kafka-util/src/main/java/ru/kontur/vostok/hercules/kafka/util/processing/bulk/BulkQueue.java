package ru.kontur.vostok.hercules.kafka.util.processing.bulk;

import ru.kontur.vostok.hercules.kafka.util.processing.BackendServiceFailedException;
import ru.kontur.vostok.hercules.kafka.util.processing.SinkStatusFsm;
import ru.kontur.vostok.hercules.util.functional.Result;

import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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

    private static final int STOPPED_CHECK_TIMEOUT_MS = 10;

    private final BlockingQueue<BulkQueue.RunUnit<Key, Value>> queue;
    private final Queue<Future<Result<RunResult<Key, Value>, BackendServiceFailedException>>> commitQueue = new LinkedList<>();
    private final SinkStatusFsm status;

    public BulkQueue(int queueSize, SinkStatusFsm status) {
        this.queue = new ArrayBlockingQueue<>(queueSize);
        this.status = status;
    }

    public void put(RecordStorage<Key, Value> storage) {
        CompletableFuture<Result<BulkQueue.RunResult<Key, Value>, BackendServiceFailedException>> future = new CompletableFuture<>();
        RunUnit<Key, Value> unit = new RunUnit<>(storage, future);

        try {
            while (status.isRunning() && !queue.offer(unit, STOPPED_CHECK_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {/* empty */}
        } catch (InterruptedException e) {
            throw new RuntimeException("Should never happened", e);
        }
        commitQueue.add(future);
    }

    public RunUnit<Key, Value> take() {
        RunUnit<Key, Value> result;
        try {

            do {
                result = queue.poll(STOPPED_CHECK_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            } while (status.isRunning() && Objects.isNull(result));
        } catch (InterruptedException e) {
            throw new RuntimeException("Should never happened", e);
        }

        return result;
    }

    public BulkQueue.RunResult<Key, Value> getLastCommitOrNull() throws BackendServiceFailedException {
        int processed = 0;
        int dropped = 0;
        Result<BulkQueue.RunResult<Key, Value>, BackendServiceFailedException> result = Result.ok(null);
        while (status.isRunning() && !commitQueue.isEmpty() && commitQueue.element().isDone()) {
            try {
                result = commitQueue.remove().get(0, TimeUnit.MILLISECONDS);
            } catch (InterruptedException | TimeoutException | ExecutionException e) {
                throw new RuntimeException("Should never happened", e);
            }

            if (!result.isOk()) {
                throw result.getError();
            }

            BulkSenderStat stat = result.get().getStat();
            processed += stat.getProcessed();
            dropped += stat.getDropped();
        }
        if (Objects.isNull(result.get())) {
            return null;
        } else {
            return new RunResult<>(result.get().storage, new BulkSenderStat(processed, dropped));
        }
    }
}
