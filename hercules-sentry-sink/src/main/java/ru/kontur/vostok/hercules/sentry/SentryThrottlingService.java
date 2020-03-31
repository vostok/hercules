package ru.kontur.vostok.hercules.sentry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.health.Meter;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.health.MetricsUtil;
import ru.kontur.vostok.hercules.throttling.rate.SlidingRateLimiter;
import ru.kontur.vostok.hercules.util.parameter.Parameter;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;
import ru.kontur.vostok.hercules.util.validation.LongValidators;

import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Gregory Koshelev
 */
public class SentryThrottlingService {
    private static final Logger LOGGER = LoggerFactory.getLogger(SentryThrottlingService.class);

    private final ConcurrentHashMap<String, SlidingRateLimiter> rateLimiters = new ConcurrentHashMap<>(128);

    private final long limit;
    private final long timeWindowMs;

    private final MetricsCollector metricsCollector;
    private final ConcurrentHashMap<String, Meter> rejectedEventMeterMap = new ConcurrentHashMap<>();

    public SentryThrottlingService(Properties properties, MetricsCollector metricsCollector) {
        this.limit = PropertiesUtil.get(Props.LIMIT, properties).get();
        this.timeWindowMs = PropertiesUtil.get(Props.TIME_WINDOW_MS, properties).get();

        this.metricsCollector = metricsCollector;
    }

    public boolean check(String organization, long eventTimestampMs) {
        SlidingRateLimiter rateLimiter =
                rateLimiters.computeIfAbsent(
                        organization,
                        k -> new SlidingRateLimiter(limit, timeWindowMs, eventTimestampMs));
        boolean result = rateLimiter.updateAndCheck(eventTimestampMs);
        if (!result) {
            LOGGER.debug("Event in organization '{}' has been rejected by rate limiter", organization);
            Meter rejectedEventMeter =
                    rejectedEventMeterMap.computeIfAbsent(
                            organization,
                            org -> metricsCollector.meter(MetricsUtil.toMetricPath("rateLimit", "organization", org, "rejectedEvent")));
            rejectedEventMeter.mark();
        }
        return result;
    }

    private static class Props {
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
