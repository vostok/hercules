package ru.kontur.vostok.hercules.sentry.sink;

import ru.kontur.vostok.hercules.util.parameter.Parameter;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;
import ru.kontur.vostok.hercules.util.validation.LongValidators;

import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Artem Zhdanov
 */
class RateLimitService {

    private final long timeWindowMs;
    private final long limit;

    private final ConcurrentHashMap<String, Bucket> buckets;

    RateLimitService(Properties properties) {
        final TimeUnit timeUnit = PropertiesUtil.get(Props.TIME_UNIT, properties).get();
        this.timeWindowMs = timeUnit.toMillis(PropertiesUtil.get(Props.TIME_WINDOW, properties).get());

        this.limit = PropertiesUtil.get(Props.LIMIT, properties).get();
        this.buckets = new ConcurrentHashMap<>();
    }

    /**
     * @param key - key of rate limit state.
     * @return true - if rate limit is not exceeded, else false
     */
    boolean updateAndCheck(String key) {
        if (key == null) {
            return false;
        }
        return buckets.computeIfAbsent(key, s -> new Bucket(limit, timeWindowMs)).update() >= 0;
    }

    private static class Bucket {

        private final long limit;
        private final long timeWindow;

        private final AtomicLong value;
        private final AtomicLong lastUpdate;

        Bucket(long limit, long timeWindow) {
            this.limit = limit;
            this.timeWindow = timeWindow;
            this.value = new AtomicLong(this.limit * this.timeWindow);
            this.lastUpdate = new AtomicLong(System.currentTimeMillis());
        }

        long update() {
            long now = System.currentTimeMillis();
            long delta = now - lastUpdate.getAndUpdate(operand -> now);
            long increase = delta * limit - timeWindow;
            return value.updateAndGet(val -> Math.max(-1L, Math.min(val + increase, limit * timeWindow)));
        }
    }

    private static class Props {
        static final Parameter<Long> LIMIT = Parameter
                .longParameter("limit")
                .withDefault(1000L)
                .withValidator(LongValidators.positive())
                .build();

        static final Parameter<Long> TIME_WINDOW = Parameter
                .longParameter("timeWindow")
                .withDefault(1L)
                .build();

        static final Parameter<TimeUnit> TIME_UNIT = Parameter
                .enumParameter("timeUnit", TimeUnit.class)
                .withDefault(TimeUnit.MINUTES)
                .build();

    }

}
