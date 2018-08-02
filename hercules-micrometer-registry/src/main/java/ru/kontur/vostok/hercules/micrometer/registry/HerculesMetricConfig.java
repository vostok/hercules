package ru.kontur.vostok.hercules.micrometer.registry;

import io.micrometer.core.instrument.dropwizard.DropwizardConfig;
import io.micrometer.core.lang.Nullable;
import java.util.concurrent.TimeUnit;
import ru.kontur.vostok.hercules.gateway.client.EventQueue;

/**
 * @author Daniil Zhenikhov
 */
public interface HerculesMetricConfig extends DropwizardConfig {
    /**
     * Accept configuration defaults
     */
    HerculesMetricConfig DEFAULT = k -> null;

    /**
     * Get the value associated with a key.
     *
     * @param key Key to lookup in the config.
     * @return Value for the key or null if no key is present.
     */
    @Nullable
    String get(String key);

    /**
     * @return Property prefix to prepend to configuration names.
     */
    default String prefix() {
        return "hercules.metrics";
    }

    /**
     * @return Property prefix to event queue configuration names.
     */
    default String prefixEventQueueName() {
        return prefix() + ".queue";
    }

    /**
     * @return Configured event queue
     */
    default EventQueue eventQueue() {
        String prefixQueueName = prefixEventQueueName();
        String name = get(prefixQueueName + ".name");
        String stream = get(prefixQueueName + ".stream");
        long periodMillis = Long.parseLong(get(prefixQueueName + ".periodMillis"));
        int batchSize = Integer.parseInt(get(prefixQueueName + ".batchSize"));
        int capacity = Integer.parseInt(get(prefixQueueName + ".capacity"));
        boolean loseOnOverflow = Boolean.parseBoolean(get(prefixQueueName + ".loseOnOverflow"));

        return new EventQueue(name,
                stream,
                periodMillis,
                batchSize,
                capacity,
                loseOnOverflow);
    }

    /**
     * @return For the default naming convention, turn the specified tag keys into
     * part of the metric prefix.
     */
    default String[] tagsAsPrefix() {
        return new String[0];
    }

    default TimeUnit rateUnits() {
        String v = get(prefix() + ".rateUnits");
        return v == null ? TimeUnit.SECONDS : TimeUnit.valueOf(v.toUpperCase());
    }

    default TimeUnit durationUnits() {
        String v = get(prefix() + ".durationUnits");
        return v == null ? TimeUnit.MILLISECONDS : TimeUnit.valueOf(v.toUpperCase());
    }

    /**
     * @return {@code true} if publishing is enabled. Default is {@code true}.
     */
    default boolean enabled() {
        String v = get(prefix() + ".enabled");
        return v == null || Boolean.valueOf(v);
    }
}
