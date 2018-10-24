package ru.kontur.vostok.hercules.kafka.util.processing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.configuration.util.PropertiesUtil;
import ru.kontur.vostok.hercules.util.properties.PropertyDescription;
import ru.kontur.vostok.hercules.util.properties.PropertyDescriptions;
import ru.kontur.vostok.hercules.util.validation.Validators;

import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * BulkSenderPool
 *
 * @author Kirill Sulim
 */
public class BulkSenderPool<Key, Value> {

    private static class Props {
        static final String SENDER_POOL_SCOPE = "senderPool";

        static final PropertyDescription<Integer> POOL_SIZE = PropertyDescriptions
                .integerProperty("size")
                .withDefaultValue(4)
                .withValidator(Validators.greaterThan(0))
                .build();

        static final PropertyDescription<Integer> SHUTDOWN_TIMEOUT_MS = PropertyDescriptions
                .integerProperty("shutdownTimeoutMs")
                .withDefaultValue(5_000)
                .withValidator(Validators.greaterOrEquals(0))
                .build();
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(BulkSenderPool.class);

    private final int poolSize;
    private final int shutdownTimeoutMs;

    private final ExecutorService pool;
    private final Supplier<BulkSender<Value>> senderFactory;
    private final BulkQueue<Key, Value> queue;
    private final CommonBulkSinkStatusFsm status;


    public BulkSenderPool(
            final String id,
            final Properties sinkProperties,
            final BulkQueue<Key, Value> queue,
            final Supplier<BulkSender<Value>> senderFactory,
            final CommonBulkSinkStatusFsm status
    ) {
        final Properties senderPoolProperties = PropertiesUtil.ofScope(sinkProperties, Props.SENDER_POOL_SCOPE);

        this.poolSize = Props.POOL_SIZE.extract(senderPoolProperties);
        this.shutdownTimeoutMs = Props.SHUTDOWN_TIMEOUT_MS.extract(senderPoolProperties);

        this.pool = Executors.newFixedThreadPool(poolSize, new NamedThreadFactory(id + "-sender-pool"));

        this.senderFactory = senderFactory;
        this.queue = queue;
        this.status = status;
    }

    public void start() {
        for (int i = 0; i < poolSize; ++i) {
            pool.execute(new BulkSenderRunnable<>(senderFactory.get(), queue, status));
        }
    }

    public void stop() throws InterruptedException {
        pool.shutdown();
        if (!pool.awaitTermination(shutdownTimeoutMs, TimeUnit.MILLISECONDS)) {
            LOGGER.warn("Sender pool was terminated by force");
        }
    }
}

