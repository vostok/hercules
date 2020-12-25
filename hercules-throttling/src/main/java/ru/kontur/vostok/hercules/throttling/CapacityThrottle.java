package ru.kontur.vostok.hercules.throttling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.util.concurrent.LifoSemaphore;
import ru.kontur.vostok.hercules.util.parameter.Parameter;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;
import ru.kontur.vostok.hercules.util.validation.LongValidators;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * @author Gregory Koshelev
 */
public class CapacityThrottle<R> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CapacityThrottle.class);

    private final long capacity;
    private final long requestTimeout;

    private final RequestWeigher<R> weigher;

    private final LifoSemaphore semaphore;

    /**
     * @param properties configuration properties
     * @param weigher    request's weigher to weigh resources are used to process request
     */
    public CapacityThrottle(
            Properties properties,
            RequestWeigher<R> weigher) {
        this.capacity = PropertiesUtil.get(Props.CAPACITY, properties).get();
        this.requestTimeout = PropertiesUtil.get(Props.REQUEST_TIMEOUT_MS, properties).get();

        this.weigher = weigher;

        this.semaphore = new LifoSemaphore(capacity);
    }

    /**
     * Throttle a request
     *
     * @param request a request
     */
    public ThrottleResult throttle(R request) {
        int weight = weigher.weigh(request);
        if (weight < 0) {
            throw new IllegalStateException("Request is invalid");
        }
        if (semaphore.tryAcquire(weight)) {
            return ThrottleResult.passed(weight);
        }
        boolean acquired;
        try {
            acquired = semaphore.tryAcquire(weight, requestTimeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            return ThrottleResult.interrupted();
        }
        if (acquired) {
            return ThrottleResult.passed(weight);
        } else {
            return ThrottleResult.expired();
        }
    }

    public void release(ThrottleResult result) {
        semaphore.release(result.capacity());
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
