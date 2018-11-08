package ru.kontur.vostok.hercules.throttling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.util.properties.PropertyDescription;
import ru.kontur.vostok.hercules.util.properties.PropertyDescriptions;
import ru.kontur.vostok.hercules.util.validation.Validators;

import java.util.Properties;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * @author Gregory Koshelev
 */
public class CapacityThrottle<R, C> implements Throttle<R, C> {

    private static class Props {
        static final PropertyDescription<Long> CAPACITY = PropertyDescriptions
                .longProperty(ThrottlingProperties.CAPACITY)
                .withDefaultValue(ThrottlingDefaults.DEFAULT_CAPACITY)
                .withValidator(Validators.greaterThan(0L))
                .build();

        static final PropertyDescription<Long> REQUEST_TIMEOUT_MS = PropertyDescriptions
                .longProperty(ThrottlingProperties.REQUEST_TIMEOUT)
                .withDefaultValue(ThrottlingDefaults.DEFAULT_REQUEST_TIMEOUT)
                .withValidator(Validators.greaterThan(0L))
                .build();
    }

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
        this.capacity = Props.CAPACITY.extract(properties);
        this.requestTimeout = Props.REQUEST_TIMEOUT_MS.extract(properties);

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
