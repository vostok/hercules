package ru.kontur.vostok.hercules.throttling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private final long capacity;
    private final long requestTimeout;

    private final RequestWeigher<R> weigher;
    private final RequestProcessor<R, C> requestProcessor;
    private final ThrottledRequestProcessor<R> throttledRequestProcessor;

    private final Semaphore semaphore;

    /**
     * @param capacity                  total amount of throttled resources
     * @param requestTimeout            request's timeout. Timeout is measured in milliseconds
     * @param weigher                   request's weigher to weigh resources are used to process request
     * @param requestProcessor          processes requests
     * @param throttledRequestProcessor processes throttled (discarded by some reasons) requests
     */
    public CapacityThrottle(long capacity, long requestTimeout, RequestWeigher<R> weigher, RequestProcessor<R, C> requestProcessor, ThrottledRequestProcessor<R> throttledRequestProcessor) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity should be positive");
        }
        if (requestTimeout <= 0) {
            throw new IllegalArgumentException("RequestTimeout should be posititve");
        }
        this.capacity = capacity;
        this.requestTimeout = requestTimeout;

        this.weigher = weigher;
        this.requestProcessor = requestProcessor;
        this.throttledRequestProcessor = throttledRequestProcessor;

        this.semaphore = new Semaphore(capacity > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) capacity);
    }

    /**
     * Asynchronously throttle request
     *
     * @param request to be throttled
     * @param context is additional request's data
     */
    @Override
    public void throttleAsync(R request, C context) {
        int weight = weigher.weigh(request);
        if (weight < 0) {
            throw new IllegalStateException("Request is invalid");
        }
        if (semaphore.tryAcquire(weight)) {
            requestProcessor.processAsync(request, context, () -> semaphore.release(weight));
            return;
        }
        boolean acquired;
        try {
            acquired = semaphore.tryAcquire(weight, requestTimeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            throttledRequestProcessor.processAsync(request, ThrottledBy.INTERRUPTION);
            return;
        }
        if (acquired) {
            requestProcessor.processAsync(request, context, () -> semaphore.release(weight));
        } else {
            throttledRequestProcessor.processAsync(request, ThrottledBy.EXPIRATION);
        }
    }

    @Override
    public void shutdown(long timeout, TimeUnit unit) {
    }
}
