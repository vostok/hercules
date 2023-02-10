package ru.kontur.vostok.hercules.sentry.sink;

import com.fasterxml.jackson.databind.ObjectMapper;
import ru.kontur.vostok.hercules.application.Application;
import ru.kontur.vostok.hercules.application.Container;
import ru.kontur.vostok.hercules.configuration.Scopes;
import ru.kontur.vostok.hercules.curator.CuratorClient;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.routing.Router;
import ru.kontur.vostok.hercules.routing.config.zk.ZookeeperConfigurationWatchTask;
import ru.kontur.vostok.hercules.routing.config.zk.ZookeeperReadRepository;
import ru.kontur.vostok.hercules.routing.engine.tree.DecisionTreeEngineConfigDeserializer;
import ru.kontur.vostok.hercules.routing.engine.tree.DecisionTreeRouterEngine;
import ru.kontur.vostok.hercules.routing.sentry.SentryRouteDeserializer;
import ru.kontur.vostok.hercules.routing.sentry.SentryRouting;
import ru.kontur.vostok.hercules.sink.AbstractSinkParallelDaemon;
import ru.kontur.vostok.hercules.sink.parallel.sender.NoPrepareParallelSender;
import ru.kontur.vostok.hercules.util.parameter.Parameter;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;
import ru.kontur.vostok.hercules.util.routing.SentryDestination;

import java.util.Properties;

/**
 * @author Gregory Koshelev
 */
public class SentrySinkDaemon extends AbstractSinkParallelDaemon<NoPrepareParallelSender.NoPrepareEvents> {
    private static Router<Event, SentryDestination> router;

    /**
     * Main starting point
     */
    public static void main(String[] args) {
        Application.run(new SentrySinkDaemon(), args);
    }

    @Override
    public void init(Application application) {
        Properties properties = application.getConfig().getAllProperties();
        Container container = application.getContainer();

        Properties curatorProperties = PropertiesUtil.ofScope(properties, Scopes.CURATOR);
        CuratorClient curatorClient = container.register(new CuratorClient(curatorProperties));

        ObjectMapper objectMapper = new ObjectMapper();
        router = container.register(createRouter(properties, curatorClient, objectMapper));

        super.init(application);
    }

    @Override
    public NoPrepareParallelSender createSender(Properties senderProperties, MetricsCollector metricsCollector) {
        return new SentrySender(senderProperties, metricsCollector, router);
    }

    @Override
    public String getApplicationName() {
        return "Hercules sentry sink";
    }

    @Override
    public String getApplicationId() {
        return "sink.sentry";
    }

    private static Router<Event, SentryDestination> createRouter(
            Properties properties, CuratorClient curatorClient, ObjectMapper objectMapper
    ) {
        ZookeeperReadRepository readRepository = ZookeeperReadRepository.builder()
                .withCuratorClient(curatorClient)
                .withRootPath(SentryRouting.STORE_ROOT)
                .withRouteDeserializer(new SentryRouteDeserializer(objectMapper))
                .withConfigDeserializer(new DecisionTreeEngineConfigDeserializer(objectMapper))
                .build();
        var configWatchTask = new ZookeeperConfigurationWatchTask(readRepository);
        DefaultDestination destinationDestinationConfig = PropertiesUtil.get(Props.DEFAULT_DESTINATION_PARAMETER, properties).get();
        SentryDestination defaultDestination = destinationDestinationConfig == DefaultDestination.PROJECT_SUBPROJECT
                ? SentryDestination.byDefault()
                : SentryDestination.toNowhere();
        var engine = new DecisionTreeRouterEngine(SentryRouting.DEFAULT_CONFIG, defaultDestination);
        return new Router<>(configWatchTask, engine);
    }

    enum DefaultDestination {
        PROJECT_SUBPROJECT,
        NOWHERE,
    }

    static class Props {

        /**
         * Determines what will routing submodule use as default destination.
         */
        static final Parameter<DefaultDestination> DEFAULT_DESTINATION_PARAMETER = Parameter
                .enumParameter("routing.default.destination", DefaultDestination.class)
                .withDefault(DefaultDestination.PROJECT_SUBPROJECT)
                .build();

        private Props() {
        }
    }
}
