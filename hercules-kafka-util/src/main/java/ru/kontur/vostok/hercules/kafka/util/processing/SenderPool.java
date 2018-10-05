package ru.kontur.vostok.hercules.kafka.util.processing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.util.functional.Result;
import ru.kontur.vostok.hercules.util.properties.PropertiesExtractor;

import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.Supplier;

/**
 * SenderPool
 *
 * @author Kirill Sulim
 */
public class SenderPool<Key, Value> {

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

    private static class RunUnit<Key, Value> {
        private final RecordStorage<Key, Value> storage;
        private final CompletableFuture<Result<RunResult<Key, Value>, BackendServiceFailedException>> future;

        public RunUnit(
                RecordStorage<Key, Value> storage,
                CompletableFuture<Result<RunResult<Key, Value>, BackendServiceFailedException>> future
        ) {
            this.storage = storage;
            this.future = future;
        }
    }

    private static final String THREAD_COUNT_PROP_NAME = "pool.threadCount";
    private static final String QUEUE_SIZE_PROP_NAME = "pool.queueSize";

    private static final int DEFAULT_THREAD_COUNT = 4;
    private static final int DEFAULT_QUEUE_SIZE = 32;

    private static final Logger LOGGER = LoggerFactory.getLogger(SenderPool.class);

    private final Thread[] threads;
    private final BlockingQueue<RunUnit<Key, Value>> queue;
    private final BulkSender<Value> pinger;

    public SenderPool(
            Properties sinkPoolProperties,
            Supplier<BulkSender<Value>> senderFactory,
            CommonBulkSinkStatusFsm status
    ) {

        final int threadCount = PropertiesExtractor.getAs(sinkPoolProperties, THREAD_COUNT_PROP_NAME, Integer.class)
                .orElse(DEFAULT_THREAD_COUNT);

        final int queueSize = PropertiesExtractor.getAs(sinkPoolProperties, QUEUE_SIZE_PROP_NAME, Integer.class)
                .orElse(DEFAULT_QUEUE_SIZE);

        threads = new Thread[threadCount];
        queue = new ArrayBlockingQueue<>(queueSize);
        pinger = senderFactory.get();

        for (int i = 0; i < threads.length; ++i) {
            Thread thread = new Thread(() -> {
                BulkSender<Value> sender = senderFactory.get();
                while (status.isRunning()) {
                    try {
                        RunUnit<Key, Value> take = queue.take();
                        try {
                            BulkSenderStat stat = sender.process(take.storage.getRecords());
                            take.future.complete(Result.ok(new RunResult<>(take.storage, stat)));
                        } catch (BackendServiceFailedException e) {
                            take.future.complete(Result.error(e));
                        }
                    } catch (InterruptedException e) {
                        LOGGER.error("Interrupted", e);
                    }
                }
            });
            thread.setName(String.format("sender-pool-%d", i));
            threads[i] = thread;
            thread.start();
        }
    }

    public Future<Result<RunResult<Key, Value>, BackendServiceFailedException>> put(RecordStorage<Key, Value> storage) throws InterruptedException {
        CompletableFuture<Result<RunResult<Key, Value>, BackendServiceFailedException>> future = new CompletableFuture<>();
        queue.put(new RunUnit<>(storage, future));
        return future;
    }

    public void reset() {
        queue.clear();
    }

    public void waitWorkers() throws InterruptedException {
        for (Thread thread : threads) {
            thread.join();
        }
    }

    public boolean ping() {
        return pinger.ping();
    }
}

