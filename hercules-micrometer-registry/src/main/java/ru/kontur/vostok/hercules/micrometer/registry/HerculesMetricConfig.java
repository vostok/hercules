package ru.kontur.vostok.hercules.micrometer.registry;

import io.micrometer.core.instrument.dropwizard.DropwizardConfig;
import io.micrometer.core.lang.Nullable;
import ru.kontur.vostok.hercules.gateway.client.EventQueue;

import java.util.concurrent.TimeUnit;

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

    default String prefixEventQueue() {
        return prefix() + ".queue";
    }

    default EventQueue eventQueue() {
        String prefixQueueName = prefixEventQueue();
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

    default String host() {
        String v = get(prefix() + ".host");
        return (v == null) ? "localhost" : v;
    }

    default int port() {
        String v = get(prefix() + ".port");
        return (v == null) ? 2004 : Integer.parseInt(v);
    }

    /**
     * @return {@code true} if publishing is enabled. Default is {@code true}.
     */
    default boolean enabled() {
        String v = get(prefix() + ".enabled");
        return v == null || Boolean.valueOf(v);
    }
}
