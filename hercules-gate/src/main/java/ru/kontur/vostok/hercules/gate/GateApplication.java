package ru.kontur.vostok.hercules.gate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.application.Application;
import ru.kontur.vostok.hercules.auth.AuthManager;
import ru.kontur.vostok.hercules.configuration.PropertiesLoader;
import ru.kontur.vostok.hercules.configuration.Scopes;
import ru.kontur.vostok.hercules.configuration.util.ArgsParser;
import ru.kontur.vostok.hercules.configuration.util.PropertiesUtil;
import ru.kontur.vostok.hercules.curator.CuratorClient;
import ru.kontur.vostok.hercules.health.CommonMetrics;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.http.HttpServer;
import ru.kontur.vostok.hercules.http.HttpServerRequest;
import ru.kontur.vostok.hercules.http.handler.HttpHandler;
import ru.kontur.vostok.hercules.http.handler.RouteHandler;
import ru.kontur.vostok.hercules.meta.stream.StreamRepository;
import ru.kontur.vostok.hercules.meta.stream.StreamStorage;
import ru.kontur.vostok.hercules.partitioner.HashPartitioner;
import ru.kontur.vostok.hercules.partitioner.NaiveHasher;
import ru.kontur.vostok.hercules.sd.BeaconService;
import ru.kontur.vostok.hercules.throttling.CapacityThrottle;
import ru.kontur.vostok.hercules.throttling.Throttle;
import ru.kontur.vostok.hercules.undertow.util.DefaultHttpServerRequestWeigher;
import ru.kontur.vostok.hercules.undertow.util.DefaultThrottledHttpServerRequestProcessor;
import ru.kontur.vostok.hercules.undertow.util.UndertowHttpServer;
import ru.kontur.vostok.hercules.undertow.util.handlers.InstrumentedRouteHandlerBuilder;
import ru.kontur.vostok.hercules.util.application.ApplicationContextHolder;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * @author Gregory Koshelev
 */
public class GateApplication {

    private static final Logger LOGGER = LoggerFactory.getLogger(GateApplication.class);

    private static MetricsCollector metricsCollector;
    private static HttpServer server;
    private static EventSender eventSender;
    private static CuratorClient curatorClient;
    private static AuthManager authManager;
    private static AuthValidationManager authValidationManager;
    private static BeaconService beaconService;

    public static void main(String[] args) {
        long start = System.currentTimeMillis();

        try {
            Application.run("Hercules Gate", "gate", args);

            Map<String, String> parameters = ArgsParser.parse(args);

            Properties properties = PropertiesLoader.load(parameters.getOrDefault("application.properties", "file://application.properties"));

            Properties httpServerProperties = PropertiesUtil.ofScope(properties, Scopes.HTTP_SERVER);
            Properties producerProperties = PropertiesUtil.ofScope(properties, Scopes.PRODUCER);
            Properties curatorProperties = PropertiesUtil.ofScope(properties, Scopes.CURATOR);
            Properties metricsProperties = PropertiesUtil.ofScope(properties, Scopes.METRICS);
            Properties contextProperties = PropertiesUtil.ofScope(properties, Scopes.CONTEXT);

            ApplicationContextHolder.init("Hercules Gate", "gate", contextProperties);

            metricsCollector = new MetricsCollector(metricsProperties);
            metricsCollector.start();
            CommonMetrics.registerCommonMetrics(metricsCollector);

            eventSender = new EventSender(producerProperties, new HashPartitioner(new NaiveHasher()));

            curatorClient = new CuratorClient(curatorProperties);
            curatorClient.start();

            authManager = new AuthManager(curatorClient);
            authManager.start();

            authValidationManager = new AuthValidationManager(curatorClient);
            authValidationManager.start();

            server = createHttpServer(httpServerProperties);
            server.start();

            beaconService = new BeaconService(curatorClient);
            beaconService.start();
        } catch (Throwable t) {
            LOGGER.error("Cannot start application due to", t);
            shutdown();
            return;
        }

        Runtime.getRuntime().addShutdownHook(new Thread(GateApplication::shutdown));

        LOGGER.info("Gateway started for {} millis", System.currentTimeMillis() - start);
    }

    private static void shutdown() {
        long start = System.currentTimeMillis();

        LOGGER.info("Started Gateway shutdown");
        try {
            if (beaconService != null) {
                beaconService.stop(5_000, TimeUnit.MILLISECONDS);
            }
        } catch (Throwable t) {
            LOGGER.error("Error on beacon service shutdown", t);
        }

        try {
            if (server != null) {
                server.stop(5_000, TimeUnit.MILLISECONDS);
            }
        } catch (Throwable t) {
            LOGGER.error("Error on http server shutdown", t);
            //TODO: Process error
        }

        try {
            if (eventSender != null) {
                eventSender.stop(5_000, TimeUnit.MILLISECONDS);
            }
        } catch (Throwable t) {
            LOGGER.error("Error on event sender shutdown", t);
            //TODO: Process error
        }

        try {
            if (authManager != null) {
                authManager.stop();
            }
        } catch (Throwable t) {
            LOGGER.error("Error on auth manager shutdown", t);
        }

        try {
            if (authValidationManager != null) {
                authValidationManager.stop();
            }
        } catch (Throwable t) {
            LOGGER.error("Error on stopping auth validation manager", t);
        }

        try {
            if (curatorClient != null) {
                curatorClient.stop();
            }
        } catch (Throwable t) {
            LOGGER.error("Error on curator client shutdown", t);
            //TODO: Process error
        }

        try {
            if (metricsCollector != null) {
                metricsCollector.stop();
            }
        } catch (Throwable t) {
            LOGGER.error("Error on metrics collector shutdown", t);
            //TODO: Process error
        }

        LOGGER.info("Finished Gateway shutdown for {}  millis", System.currentTimeMillis() - start);
    }

    public static HttpServer createHttpServer(Properties httpServerProperies) {
        StreamRepository streamRepository = new StreamRepository(curatorClient);
        StreamStorage streamStorage = new StreamStorage(streamRepository, 30_000L /* TODO: for test usages; It should be moved to configuration */);

        Properties throttlingProperties = PropertiesUtil.ofScope(httpServerProperies, Scopes.THROTTLING);

        SendRequestProcessor sendRequestProcessor = new SendRequestProcessor(metricsCollector, eventSender);
        Throttle<HttpServerRequest, SendContext> throttle = new CapacityThrottle<>(
                throttlingProperties,
                new DefaultHttpServerRequestWeigher(),
                sendRequestProcessor,
                new DefaultThrottledHttpServerRequestProcessor()
        );

        long maxContentLength = HttpServer.Props.MAX_CONTENT_LENGTH.extract(httpServerProperies);
        HttpHandler sendAsyncHandler = new GateHandler(metricsCollector, authManager, throttle, authValidationManager, streamStorage, true, maxContentLength);
        HttpHandler sendHandler = new GateHandler(metricsCollector, authManager, throttle, authValidationManager, streamStorage, false, maxContentLength);

        RouteHandler handler = new InstrumentedRouteHandlerBuilder(metricsCollector).
                post("/stream/sendAsync", sendAsyncHandler).
                post("/stream/send", sendHandler).
                build();

        return new UndertowHttpServer(httpServerProperies, handler);
    }
}
