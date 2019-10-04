package ru.kontur.vostok.hercules.throttling.rate;

import org.jetbrains.annotations.NotNull;
import ru.kontur.vostok.hercules.util.parameter.Parameter;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;
import ru.kontur.vostok.hercules.util.validation.LongValidators;

import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Artem Zhdanov
 * @author Gregory Koshelev
 */
public class RateLimiter {
    private final long limit;
    private final long timeWindowMs;

    private final ConcurrentMap<String, Bucket> buckets = new ConcurrentHashMap<>(128);

    public RateLimiter(Properties properties) {
        this.limit = PropertiesUtil.get(Props.LIMIT, properties).get();
        this.timeWindowMs = PropertiesUtil.get(Props.TIME_WINDOW_MS, properties).get();
    }

    public boolean updateAndCheck(@NotNull String key) {
        return buckets.computeIfAbsent(key, k -> new Bucket(limit, timeWindowMs)).updateAndGet() >= 0;
    }

    private static class Bucket {
        private final long limit;
        private final long timeWindowMs;

        private final AtomicLong value;
        private final AtomicLong lastUpdatedAt;

        Bucket(long limit, long timeWindowMs) {
            this.limit = limit;
            this.timeWindowMs = timeWindowMs;

            this.value = new AtomicLong(this.limit * this.timeWindowMs);
            this.lastUpdatedAt = new AtomicLong(System.currentTimeMillis());
        }

        long updateAndGet() {
            long now = System.currentTimeMillis();
            long delta = now - lastUpdatedAt.getAndUpdate(x -> now);
            long increase = delta * limit - timeWindowMs;
            return value.updateAndGet(val -> Math.max(-1L, Math.min(val + increase, limit * timeWindowMs)));
        }
    }

    private static class Props {
        static final Parameter<Long> LIMIT =
                Parameter.longParameter("limit").
                        withDefault(1_000L).
                        withValidator(LongValidators.positive()).
                        build();

        static final Parameter<Long> TIME_WINDOW_MS =
                Parameter.longParameter("timeWindowMs").
                        withDefault(60_000L).
                        withValidator(LongValidators.positive()).
                        build();
    }
}
