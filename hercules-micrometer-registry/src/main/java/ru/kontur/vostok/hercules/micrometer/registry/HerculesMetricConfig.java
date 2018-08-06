package ru.kontur.vostok.hercules.micrometer.registry;

import io.micrometer.core.instrument.dropwizard.DropwizardConfig;
import io.micrometer.core.lang.Nullable;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import ru.kontur.vostok.hercules.gateway.client.DefaultConfigurationConstants;
import ru.kontur.vostok.hercules.gateway.client.EventQueue;

/**
 * @author Daniil Zhenikhov
 */
public interface HerculesMetricConfig extends DropwizardConfig {
    Random RANDOM = new Random();

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
        return new EventQueue(
                queueName(),
                streamName(),
                queuePeriodMillis(),
                queueBatchSize(),
                queueCapacity(),
                queueLoseOnOverflow());
    }

    /**
     * @return For the default naming convention, turn the specified tag keys into
     * part of the metric prefix.
     */
    default String[] tagsAsPrefix() {
        return new String[0];
    }

    /**
     * @return Return name of event queue for registration.
     */
    default String queueName() {
        String v = get(prefixEventQueueName() + ".name");
        return v == null ? String.valueOf(RANDOM.nextLong()) : v;
    }

    /**
     * @return Return topic name in kafka
     */
    default String streamName() {
        String v = get(prefixEventQueueName() + ".stream");
        Objects.requireNonNull(v);

        return v;
    }

    /**
     * @return Return period in millis for event queue
     */
    default long queuePeriodMillis() {
        String v = get(prefixEventQueueName() + ".periodMillis");
        return v == null
                ? DefaultConfigurationConstants.DEFAULT_PERIOD_MILLIS
                : Long.parseLong(v);
    }

    /**
     * @return Return size of batch for event queue
     */
    default int queueBatchSize() {
        String v = get(prefixEventQueueName() + ".batchSize");
        return v == null
                ? DefaultConfigurationConstants.DEFAULT_BATCH_SIZE
                : Integer.parseInt(v);
    }

    /**
     * @return Return capacity of event queue
     */
    default int queueCapacity() {
        String v = get(prefixEventQueueName() + ".capacity");
        return v == null
                ? DefaultConfigurationConstants.DEFAULT_CAPACITY
                : Integer.parseInt(v);
    }

    /**
     * @return Return flag of event queue to lose events on overflow or not
     */
    default boolean queueLoseOnOverflow() {
        String v = get(prefixEventQueueName() + ".loseOnOverflow");
        return v == null
                ? DefaultConfigurationConstants.DEFAULT_IS_LOSE_ON_OVERFLOW
                : Boolean.parseBoolean(v);
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
