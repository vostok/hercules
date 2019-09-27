package ru.kontur.vostok.hercules.sentry.sink;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.util.parameter.Parameter;
import ru.kontur.vostok.hercules.util.parameter.ParameterValue;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;
import ru.kontur.vostok.hercules.util.validation.IntegerValidators;
import ru.kontur.vostok.hercules.util.validation.ValidationResult;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * @author Artem Zhdanov
 */
public class RateLimitService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RateLimitService.class);

    private final TimeUnit timeUnit;

    private final Integer defaultLimit;

    private final Map<String, Bucket> ruleBuckets;

    public RateLimitService(Properties properties) {
        this.timeUnit = TimeUnit.MINUTES;
        this.defaultLimit = PropertiesUtil.get(Props.LIMIT, properties).get();
        this.ruleBuckets = parseRules(PropertiesUtil.get(Props.RULES, properties));
    }

    public RateLimitService(Properties properties, TimeUnit timeUnit) {
        this.timeUnit = timeUnit;
        this.defaultLimit = PropertiesUtil.get(Props.LIMIT, properties).get();
        this.ruleBuckets = parseRules(PropertiesUtil.get(Props.RULES, properties));
    }

    /**
     * @param key - key of rate limit state
     * @return true - if rate limit is not exceeded, else false
     */
    public boolean updateAndCheck(String key) {
        if (key == null) {
            return false;
        }
        Bucket bucket = ruleBuckets.get(key);
        if (bucket == null) {
            bucket = new Bucket(defaultLimit);
            ruleBuckets.put(key, bucket);
            LOGGER.info("Rate limiting. Create new bucket with key " + key);
        }
        return bucket.update(1) >= 0;
    }

    private Map<String, Bucket> parseRules(ParameterValue<String[]> rulesParameterValue) {
        if (rulesParameterValue.isEmpty()) {
            return new HashMap<>();
        } else {
            Map<String, Bucket> bucketRules = new HashMap<>(rulesParameterValue.get().length);
            for (String rule : rulesParameterValue.get()) {
                int dIdx = rule.indexOf(":");
                String key = rule.substring(0, dIdx);
                int val = Integer.parseInt(rule.substring(++dIdx));
                bucketRules.put(key, new Bucket(val));
            }
            return bucketRules;
        }
    }

    private class Bucket {

        private int maxRate;
        private int value;
        private long lastUpdate;

        private long timeDenominator;

        Bucket(int maxRate) {
            this.maxRate = maxRate;
            this.value = maxRate;
            this.lastUpdate = System.currentTimeMillis();
            this.timeDenominator = timeUnit.toMillis(1);
        }

        int update(int n) {
            long now = System.currentTimeMillis();
            long delta = now - lastUpdate;
            int increase = (int) (delta * maxRate / timeDenominator);
            value = Math.max(-1, Math.min(maxRate, increase + value - n));
            lastUpdate = now;

            LOGGER.trace("Rate Limit Service value = {}, delta = {}, increase = {}", value, delta, increase);

            return value;
        }
    }

    private static class Props {

        private static final Pattern RULES_PROP_PATTERN = Pattern.compile("[a-zA-Z\\d]+:\\d+");

        static final Parameter<Integer> LIMIT = Parameter
                .integerParameter("limit")
                .withDefault(1000)
                .withValidator(IntegerValidators.positive())
                .build();

        static final Parameter<String[]> RULES = Parameter
                .stringArrayParameter("rules")
                .withValidator(value -> Stream.of(value).allMatch(s -> RULES_PROP_PATTERN.matcher(s).matches())
                        ? ValidationResult.ok()
                        : ValidationResult.error("The rule of rate limiting has invalid format."))
                .build();
    }

}
