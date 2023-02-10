package ru.kontur.vostok.hercules.sink;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.application.Container;
import ru.kontur.vostok.hercules.configuration.Scopes;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.sink.metrics.SinkMetrics;
import ru.kontur.vostok.hercules.util.concurrent.ThreadFactories;
import ru.kontur.vostok.hercules.util.lifecycle.Stoppable;
import ru.kontur.vostok.hercules.util.parameter.Parameter;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;

import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Base Sink starter. Uses pool of SenderSink for concurrent processing.
 *
 * @author Innokentiy Krivonosov
 */
public class BaseSinkDaemonStarter implements Stoppable {
    private static final Logger LOGGER = LoggerFactory.getLogger(BaseSinkDaemonStarter.class);

    private ExecutorService executor;
    private final Sender sender;
    private final String daemonId;
    private final MetricsCollector metricsCollector;

    public BaseSinkDaemonStarter(Sender sender, String daemonId, MetricsCollector metricsCollector) {
        this.sender = sender;
        this.daemonId = daemonId;
        this.metricsCollector = metricsCollector;
    }

    public void start(Properties properties, Container container) {
        Properties sinkProperties = PropertiesUtil.ofScope(properties, Scopes.SINK);
        SinkMetrics sinkMetrics = new SinkMetrics(metricsCollector);

        int poolSize = PropertiesUtil.get(Props.POOL_SIZE, sinkProperties).get();
        executor = Executors.newFixedThreadPool(poolSize, ThreadFactories.newNamedThreadFactory("sink", false));

        container.register(this);

        container.register(new SinkPool(
                poolSize,
                () -> new SenderSink(
                        executor,
                        daemonId,
                        sinkProperties,
                        sender,
                        sinkMetrics)));
    }

    @Override
    public boolean stop(long timeout, TimeUnit timeUnit) {
        try {
            if (executor != null) {
                executor.shutdown();
                boolean awaitTermination = executor.awaitTermination(timeout, timeUnit);
                if (!awaitTermination) {
                    executor.shutdownNow();
                    return executor.awaitTermination(timeout, timeUnit);
                } else {
                    return true;
                }
            } else {
                return true;
            }
        } catch (Throwable t) {
            LOGGER.error("Error on stopping sink thread executor", t);
            return false;
        }
    }

    private static class Props {
        static final Parameter<Integer> POOL_SIZE =
                Parameter.integerParameter("poolSize")
                        .withDefault(1)
                        .build();
    }
}
