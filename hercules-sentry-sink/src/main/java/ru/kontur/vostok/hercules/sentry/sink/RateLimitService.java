package ru.kontur.vostok.hercules.sentry.sink;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.util.parameter.Parameter;
import ru.kontur.vostok.hercules.util.parameter.ParameterValue;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;
import ru.kontur.vostok.hercules.util.validation.IntegerValidators;

import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Artem Zhdanov
 */
class RateLimitService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RateLimitService.class);

    private final long timeWindowMs;
    private final int maxRate;

    private final ConcurrentHashMap<String, Bucket> buckets;

    RateLimitService(Properties properties) {
        final TimeUnit timeUnit = PropertiesUtil.get(Props.TIME_UNIT, properties).get();
        this.timeWindowMs = timeUnit.toMillis(PropertiesUtil.get(Props.TIME_WINDOW, properties).get());

        this.maxRate = PropertiesUtil.get(Props.LIMIT, properties).get();
        this.buckets = new ConcurrentHashMap<>();
    }

    /**
     * @param key - key of rate limit state
     * @return true - if rate limit is not exceeded, else false
     */
    boolean updateAndCheck(String key) {
        if (key == null) {
            return false;
        }
        return buckets.computeIfAbsent(key, s -> new Bucket(maxRate, timeWindowMs)).update() > -1;
    }

    float getValue(String key) {
        return Optional.ofNullable(buckets.get(key)).map(Bucket::getValue).orElse(0f);
    }

    private static class Bucket {

        private final float maxRate;
        private final long timeDenominator;

        private AtomicReference<Float> value;
        private AtomicLong lastUpdate;


        Bucket(int maxRate, long timeWindow) {
            this.maxRate = Integer.valueOf(maxRate).floatValue();
            this.value = new AtomicReference<>(this.maxRate);
            this.timeDenominator = timeWindow;

            this.lastUpdate = new AtomicLong(System.currentTimeMillis());
        }

        int update() {
            long now = System.currentTimeMillis();
            long delta = now - lastUpdate.getAndUpdate(operand -> now);
            float increase = delta * maxRate / timeDenominator;
            float updatedValue = value.updateAndGet(val -> Math.max(-1, Math.min(maxRate, increase + val - 1)));

            LOGGER.trace("Rate Limit Service value = {}, delta = {}, increase = {}", updatedValue, delta, increase);

            return Math.round(updatedValue);
        }

        float getValue() {
            return value.get();
        }
    }

    private static class Props {
        static final Parameter<Integer> LIMIT = Parameter
                .integerParameter("limit")
                .withDefault(1000)
                .withValidator(IntegerValidators.positive())
                .build();

        static final Parameter<Long> TIME_WINDOW = Parameter
                .longParameter("timeWindow")
                .withDefault(1L)
                .build();

        static final Parameter<TimeUnit> TIME_UNIT = Parameter
                .parameter("timeUnit", val -> Optional.ofNullable(val)
                        .map(v -> ParameterValue.of(TimeUnit.valueOf(v)))
                        .orElse(ParameterValue.empty()))
                .withDefault(TimeUnit.MINUTES)
                .build();

    }

}
