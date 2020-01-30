package ru.kontur.vostok.hercules.throttling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.util.parameter.Parameter;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;
import ru.kontur.vostok.hercules.util.validation.LongValidators;

import java.util.Properties;
import java.util.concurrent.Semaphore;
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
     * @param properties                configuration properties
     * @param weigher                   request's weigher to weigh resources are used to process request
     * @param requestProcessor          processes requests
     * @param throttledRequestProcessor processes throttled (discarded by some reasons) requests
     */
    public CapacityThrottle(
            Properties properties,
            RequestWeigher<R> weigher,
            RequestProcessor<R, C> requestProcessor,
            ThrottledRequestProcessor<R> throttledRequestProcessor
    ) {
        this.capacity = PropertiesUtil.get(Props.CAPACITY, properties).get();
        this.requestTimeout = PropertiesUtil.get(Props.REQUEST_TIMEOUT_MS, properties).get();

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

    private static class Props {
        static final Parameter<Long> CAPACITY =
                Parameter.longParameter(ThrottlingProperties.CAPACITY).
                        withDefault(ThrottlingDefaults.DEFAULT_CAPACITY).
                        withValidator(LongValidators.positive()).
                        build();

        static final Parameter<Long> REQUEST_TIMEOUT_MS =
                Parameter.longParameter(ThrottlingProperties.REQUEST_TIMEOUT).
                        withDefault(ThrottlingDefaults.DEFAULT_REQUEST_TIMEOUT).
                        withValidator(LongValidators.positive()).
                        build();
    }

    public long totalCapacity() {
        return capacity;
    }

    public long availableCapacity() {
        return semaphore.availablePermits();
    }
}
