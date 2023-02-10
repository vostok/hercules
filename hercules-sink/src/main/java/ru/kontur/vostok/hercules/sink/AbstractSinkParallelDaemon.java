package ru.kontur.vostok.hercules.sink;

import ru.kontur.vostok.hercules.application.Application;
import ru.kontur.vostok.hercules.application.ApplicationRunner;
import ru.kontur.vostok.hercules.application.Container;
import ru.kontur.vostok.hercules.configuration.Scopes;
import ru.kontur.vostok.hercules.health.CommonMetrics;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.sink.parallel.EventsBatchListener;
import ru.kontur.vostok.hercules.sink.parallel.NoOpEventsBatchListener;
import ru.kontur.vostok.hercules.sink.parallel.ParallelSinkDaemonStarter;
import ru.kontur.vostok.hercules.sink.parallel.sender.AbstractParallelSender;
import ru.kontur.vostok.hercules.sink.parallel.sender.PreparedData;
import ru.kontur.vostok.hercules.undertow.util.servers.DaemonHttpServer;
import ru.kontur.vostok.hercules.util.parameter.Parameter;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;

import java.util.Properties;

/**
 * Parallel Daemon application
 * <p>
 * Supports both implementations, configurable by parameter SinkType:
 * <ul>
 * <li>BASE - BaseSinkDaemonStarter</li>
 * <li>PARALLEL - ParallelSinkDaemonStarter</li>
 * </ul>
 * <p>
 * Required sender extend from ParallelSender or NoPrepareParallelSender
 *
 * @author Innokentiy Krivonosov
 */
public abstract class AbstractSinkParallelDaemon<T extends PreparedData> implements ApplicationRunner {

    @Override
    public void init(Application application) {
        MetricsCollector metricsCollector = initMetricsCollector(application);
        initSink(application, metricsCollector);
    }

    public static MetricsCollector initMetricsCollector(Application application) {
        Properties properties = application.getConfig().getAllProperties();
        Properties metricsProperties = PropertiesUtil.ofScope(properties, Scopes.METRICS);

        MetricsCollector metricsCollector = application.getContainer().register(new MetricsCollector(metricsProperties));
        CommonMetrics.registerCommonMetrics(metricsCollector);
        return metricsCollector;
    }

    public void initSink(Application application, MetricsCollector metricsCollector) {
        Properties properties = application.getConfig().getAllProperties();
        Properties httpServerProperties = PropertiesUtil.ofScope(properties, Scopes.HTTP_SERVER);
        Properties sinkProperties = PropertiesUtil.ofScope(properties, Scopes.SINK);
        Properties senderProperties = PropertiesUtil.ofScope(sinkProperties, Scopes.SENDER);

        Container container = application.getContainer();

        AbstractParallelSender<T> sender = container.register(createSender(senderProperties, metricsCollector));

        SinkType sinkType = PropertiesUtil.get(Props.SINK_TYPE, sinkProperties).get();
        if (sinkType == SinkType.BASE) {
            new BaseSinkDaemonStarter(sender, getApplicationId(), metricsCollector).start(properties, container);
        } else {
            EventsBatchListener<T> eventsBatchListener = eventsBatchListener(metricsCollector);
            new ParallelSinkDaemonStarter<>(sender, eventsBatchListener, getApplicationName(), metricsCollector).start(properties, container);
        }

        container.register(new DaemonHttpServer(httpServerProperties, metricsCollector));
    }

    protected abstract AbstractParallelSender<T> createSender(Properties senderProperties, MetricsCollector metricsCollector);

    protected EventsBatchListener<T> eventsBatchListener(MetricsCollector metricsCollector) {
        return new NoOpEventsBatchListener<>();
    }

    private enum SinkType {
        BASE,
        PARALLEL
    }

    private static class Props {
        static final Parameter<SinkType> SINK_TYPE =
                Parameter.enumParameter("sinkType", SinkType.class)
                        .withDefault(SinkType.BASE)
                        .build();
    }
}
