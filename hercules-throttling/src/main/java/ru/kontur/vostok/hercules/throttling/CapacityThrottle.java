package ru.kontur.vostok.hercules.throttling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author Gregory Koshelev
 */
public class CapacityThrottle<R, C> implements Throttle<R, C> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CapacityThrottle.class);

    private final int capacity;
    private final int threshold;
    private final int poolSize;
    private final int requestQueueSize;
    private final long requestTimeout;

    private final RequestWeigher<R> weigher;
    private final RequestProcessor<R, C> requestProcessor;
    private final ThrottledRequestProcessor<R> throttledRequestProcessor;

    private final Semaphore semaphore;
    private final ExecutorService executor;

    /**
     * @param capacity                  total amount of throttled resources
     * @param threshold                 simple throttling strategy (get'n'process) is used when available resources is greater than threshold.
     *                                  Threshold is measured in percents of total amount of resources
     * @param poolSize                  amount of threads are  used to process request's queue
     * @param requestQueueSize          size of request's queue
     * @param requestTimeout            request's timeout. Timeout is measured in milliseconds
     * @param weigher                   request's weigher to weigh resources are used to process request
     * @param requestProcessor          processes requests
     * @param throttledRequestProcessor processes throttled (discarded by some reasons) requests
     */
    public CapacityThrottle(int capacity, int threshold, int poolSize, int requestQueueSize, long requestTimeout, RequestWeigher<R> weigher, RequestProcessor<R, C> requestProcessor, ThrottledRequestProcessor<R> throttledRequestProcessor) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity should be positive");
        }
        if (threshold < 0 && threshold > 100) {
            throw new IllegalArgumentException("Threshold should be in range [0, 100]");
        }
        if (poolSize <= 0) {
            throw new IllegalArgumentException("PoolSize should be positive");
        }
        if (requestQueueSize <= 0) {
            throw new IllegalArgumentException("RequestQueueSize should be positive");
        }
        if (requestTimeout <= 0) {
            throw new IllegalArgumentException("RequestTimeout should be posititve");
        }
        this.capacity = capacity;
        this.threshold = threshold;
        this.poolSize = poolSize;
        this.requestQueueSize = requestQueueSize;
        this.requestTimeout = requestTimeout;

        this.weigher = weigher;
        this.requestProcessor = requestProcessor;
        this.throttledRequestProcessor = throttledRequestProcessor;

        this.semaphore = new Semaphore(capacity);
        this.executor = new ThreadPoolExecutor(
                poolSize,
                poolSize,
                0L,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(requestQueueSize),
                Executors.defaultThreadFactory());
    }

    /**
     * Asynchronously throttle request
     *
     * @param request
     * @param context request context
     */
    @Override
    public void throttleAsync(R request, C context) {
        int weight = weigher.weigh(request);
        if (weight < 0) {
            throw new IllegalArgumentException("Request is invalid");
        }
        int available = semaphore.availablePermits();
        if (threshold * capacity / 100 < available && semaphore.tryAcquire(weight)) {
            requestProcessor.processAsync(request, context, () -> semaphore.release(weight));
            return;
        }
        long expiration = System.currentTimeMillis() + requestTimeout;
        try {
            executor.submit(() -> {
                if (expiration < System.currentTimeMillis()) {
                    throttledRequestProcessor.processAsync(request, ThrottledBy.EXPIRATION);
                }
                semaphore.acquireUninterruptibly(weight);
                if (expiration < System.currentTimeMillis()) {
                    try {
                        throttledRequestProcessor.processAsync(request, ThrottledBy.EXPIRATION);
                    } finally {
                        semaphore.release(weight);
                    }
                    return;
                }
                requestProcessor.processAsync(request, context, () -> semaphore.release(weight));
            });
        } catch (RejectedExecutionException exception) {
            throttledRequestProcessor.processAsync(request, ThrottledBy.QUEUE_OVERFLOW);
        }
    }

    @Override
    public void shutdown(long timeout, TimeUnit unit) {
        executor.shutdown();
        try {
            executor.awaitTermination(timeout, unit);
        } catch (InterruptedException e) {
            e.printStackTrace();//TODO: Log shutdown process and process exception
        }
    }
}
