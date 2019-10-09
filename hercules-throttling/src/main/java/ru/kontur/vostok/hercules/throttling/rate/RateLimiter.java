package ru.kontur.vostok.hercules.throttling.rate;

import org.jetbrains.annotations.NotNull;
import ru.kontur.vostok.hercules.util.parameter.Parameter;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;
import ru.kontur.vostok.hercules.util.time.TimeSource;
import ru.kontur.vostok.hercules.util.validation.LongValidators;

import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * RateLimiter is used to throttle access to some limited resources by key with fixed rate limit.
 *
 * @author Artem Zhdanov
 * @author Gregory Koshelev
 */
public class RateLimiter {
    private final TimeSource time;

    private final long limit;
    private final long timeWindowMs;

    private final ConcurrentMap<String, Bucket> buckets = new ConcurrentHashMap<>(128);

    public RateLimiter(Properties properties) {
        this(TimeSource.SYSTEM, properties);
    }

    RateLimiter(TimeSource time, Properties properties) {
        this.time = time;

        this.limit = PropertiesUtil.get(Props.LIMIT, properties).get();
        this.timeWindowMs = PropertiesUtil.get(Props.TIME_WINDOW_MS, properties).get();
    }

    /**
     * Update rate and check if rate does not exceed limit for specified key.
     *
     * @param key the resource key with limited access rate
     * @return {@code true} if rate does not exceed limit for specified key, otherwise {@code false}
     */
    public boolean updateAndCheck(@NotNull String key) {
        return buckets.computeIfAbsent(key, k -> new Bucket()).updateAndGet() >= 0;
    }

    private class Bucket {
        private final AtomicLong value;
        private final AtomicLong lastUpdatedAt;

        Bucket() {
            this.value = new AtomicLong(limit * timeWindowMs);
            this.lastUpdatedAt = new AtomicLong(time.milliseconds());
        }

        long updateAndGet() {
            long now = time.milliseconds();
            long delta = now - lastUpdatedAt.getAndUpdate(x -> now);
            long increase = delta * limit - timeWindowMs;
            return value.updateAndGet(val -> Math.max(-1L, Math.min(val + increase, limit * timeWindowMs)));
        }
    }

    public static class Props {
        public static final Parameter<Long> LIMIT =
                Parameter.longParameter("limit").
                        withDefault(1_000L).
                        withValidator(LongValidators.positive()).
                        build();

        public static final Parameter<Long> TIME_WINDOW_MS =
                Parameter.longParameter("timeWindowMs").
                        withDefault(60_000L).
                        withValidator(LongValidators.positive()).
                        build();
    }
}
