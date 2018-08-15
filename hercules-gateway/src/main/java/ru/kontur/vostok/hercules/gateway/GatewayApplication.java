package ru.kontur.vostok.hercules.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.auth.AuthManager;
import ru.kontur.vostok.hercules.configuration.Scopes;
import ru.kontur.vostok.hercules.configuration.util.ArgsParser;
import ru.kontur.vostok.hercules.configuration.util.PropertiesReader;
import ru.kontur.vostok.hercules.configuration.util.PropertiesUtil;
import ru.kontur.vostok.hercules.meta.curator.CuratorClient;
import ru.kontur.vostok.hercules.meta.stream.StreamRepository;
import ru.kontur.vostok.hercules.metrics.MetricsCollector;
import ru.kontur.vostok.hercules.partitioner.HashPartitioner;
import ru.kontur.vostok.hercules.partitioner.NaiveHasher;
import ru.kontur.vostok.hercules.util.application.shutdown.LogbackStopper;
import ru.kontur.vostok.hercules.util.application.shutdown.StackedStopper;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * @author Gregory Koshelev
 */
public class GatewayApplication {

    private static final Logger LOGGER = LoggerFactory.getLogger(GatewayApplication.class);

    public static void main(String[] args) {
        long start = System.currentTimeMillis();

        final StackedStopper stopper = new StackedStopper(new LogbackStopper());

        try {
            Map<String, String> parameters = ArgsParser.parse(args);

            Properties properties = PropertiesReader.read(parameters.getOrDefault("application.properties", "application.properties"));

            Properties httpserverProperties = PropertiesUtil.ofScope(properties, Scopes.HTTP_SERVER);
            Properties producerProperties = PropertiesUtil.ofScope(properties, Scopes.PRODUCER);
            Properties curatorProperties = PropertiesUtil.ofScope(properties, Scopes.CURATOR);
            Properties metricsProperties = PropertiesUtil.ofScope(properties, Scopes.METRICS);

            final MetricsCollector metricsCollector = new MetricsCollector(metricsProperties);
            metricsCollector.start();
            stopper.add(metricsCollector);

            final EventSender eventSender = new EventSender(producerProperties, new HashPartitioner(new NaiveHasher()));
            stopper.add(eventSender);

            final CuratorClient curatorClient = new CuratorClient(curatorProperties);
            curatorClient.start();
            stopper.add(curatorClient);

            StreamRepository streamRepository = new StreamRepository(curatorClient);

            final AuthManager authManager = new AuthManager(curatorClient);
            authManager.start();
            stopper.add(authManager);

            final HttpServer server = new HttpServer(metricsCollector, httpserverProperties, authManager, eventSender, streamRepository);
            server.start();
            stopper.add(server);
        } catch (Throwable e) {
            LOGGER.error("Cannot start application due to", e);
            stopper.stop(5_000, TimeUnit.MILLISECONDS);
            return;
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> stopper.stop(5_000, TimeUnit.MILLISECONDS)));

        LOGGER.info("Gateway started for " + (System.currentTimeMillis() - start) + " millis" );
    }
}
