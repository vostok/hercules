package ru.kontur.vostok.hercules.kafka.util.processing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.configuration.util.PropertiesUtil;
import ru.kontur.vostok.hercules.util.properties.PropertiesExtractor;

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

    private static final String SENDER_POOL_SCOPE = "senderPool";

    private static final String POOL_SIZE_PARAM = "size";
    private static final int POOL_SIZE_DEFAULT_VALUE = 4;

    private static final String SHUTDOWN_TIMEOUT_MS_PARAM = "shutdownTimeoutMs";
    private static final int SHUTDOWN_TIMEOUT_MS_DEFAULT_VALUE = 5_000;

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
        final Properties senderPoolProperties = PropertiesUtil.ofScope(sinkProperties, SENDER_POOL_SCOPE);

        this.poolSize = PropertiesExtractor.getAs(senderPoolProperties, POOL_SIZE_PARAM, Integer.class)
                .orElse(POOL_SIZE_DEFAULT_VALUE);

        this.shutdownTimeoutMs = PropertiesExtractor.getAs(senderPoolProperties, SHUTDOWN_TIMEOUT_MS_PARAM, Integer.class)
                .orElse(SHUTDOWN_TIMEOUT_MS_DEFAULT_VALUE);


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
        pool.shutdownNow();
        if (!pool.awaitTermination(shutdownTimeoutMs, TimeUnit.MILLISECONDS)) {
            LOGGER.warn("Sender pool was terminated by force");
        }
    }
}

