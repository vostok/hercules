package ru.kontur.vostok.hercules.gate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.application.Application;
import ru.kontur.vostok.hercules.auth.AdminAuthManager;
import ru.kontur.vostok.hercules.auth.AuthManager;
import ru.kontur.vostok.hercules.auth.AuthProvider;
import ru.kontur.vostok.hercules.auth.wrapper.OrdinaryAuthHandlerWrapper;
import ru.kontur.vostok.hercules.configuration.PropertiesLoader;
import ru.kontur.vostok.hercules.configuration.Scopes;
import ru.kontur.vostok.hercules.configuration.util.ArgsParser;
import ru.kontur.vostok.hercules.curator.CuratorClient;
import ru.kontur.vostok.hercules.gate.validation.EventValidator;
import ru.kontur.vostok.hercules.health.CommonMetrics;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.http.HttpServer;
import ru.kontur.vostok.hercules.http.HttpServerRequest;
import ru.kontur.vostok.hercules.http.handler.HandlerWrapper;
import ru.kontur.vostok.hercules.http.handler.HttpHandler;
import ru.kontur.vostok.hercules.http.handler.RouteHandler;
import ru.kontur.vostok.hercules.meta.stream.StreamRepository;
import ru.kontur.vostok.hercules.meta.stream.StreamStorage;
import ru.kontur.vostok.hercules.partitioner.HashPartitioner;
import ru.kontur.vostok.hercules.partitioner.NaiveHasher;
import ru.kontur.vostok.hercules.sd.BeaconService;
import ru.kontur.vostok.hercules.throttling.CapacityThrottle;
import ru.kontur.vostok.hercules.throttling.ThrottledRequestProcessor;
import ru.kontur.vostok.hercules.undertow.util.DefaultHttpServerRequestWeigher;
import ru.kontur.vostok.hercules.undertow.util.DefaultThrottledHttpServerRequestProcessor;
import ru.kontur.vostok.hercules.undertow.util.UndertowHttpServer;
import ru.kontur.vostok.hercules.undertow.util.handlers.InstrumentedRouteHandlerBuilder;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;

import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * @author Gregory Koshelev
 */
public class GateApplication {
    private static final Logger LOGGER = LoggerFactory.getLogger(GateApplication.class);

    private static MetricsCollector metricsCollector;
    private static CuratorClient curatorClient;
    private static AuthManager authManager;
    private static AuthValidationManager authValidationManager;
    private static StreamStorage streamStorage;
    private static EventSender eventSender;
    private static EventValidator eventValidator;
    private static SendRequestProcessor sendRequestProcessor;
    private static HttpServer server;
    private static BeaconService beaconService;

    public static void main(String[] args) {
        long start = System.currentTimeMillis();

        try {
            Application.run("Hercules Gate", "gate", args);

            Map<String, String> parameters = ArgsParser.parse(args);

            Properties properties = PropertiesLoader.load(parameters.getOrDefault("application.properties", "file://application.properties"));

            Properties metricsProperties = PropertiesUtil.ofScope(properties, Scopes.METRICS);
            Properties curatorProperties = PropertiesUtil.ofScope(properties, Scopes.CURATOR);
            Properties validationProperties = PropertiesUtil.ofScope(properties, "validation");
            Properties eventSenderProperties = PropertiesUtil.ofScope(properties, "gate.event.sender");
            Properties sendRequestProcessorProperties = PropertiesUtil.ofScope(properties, "gate.send.request.processor");
            Properties httpServerProperties = PropertiesUtil.ofScope(properties, Scopes.HTTP_SERVER);
            Properties sdProperties = PropertiesUtil.ofScope(properties, Scopes.SERVICE_DISCOVERY);

            metricsCollector = new MetricsCollector(metricsProperties);
            metricsCollector.start();
            CommonMetrics.registerCommonMetrics(metricsCollector);

            curatorClient = new CuratorClient(curatorProperties);
            curatorClient.start();

            authManager = new AuthManager(curatorClient);
            authManager.start();

            authValidationManager = new AuthValidationManager(curatorClient);
            authValidationManager.start();

            StreamRepository streamRepository = new StreamRepository(curatorClient);
            streamStorage = new StreamStorage(streamRepository);

            eventSender = new EventSender(eventSenderProperties, new HashPartitioner(new NaiveHasher()), metricsCollector);
            eventValidator = new EventValidator(validationProperties);

            sendRequestProcessor = new SendRequestProcessor(sendRequestProcessorProperties, eventSender, eventValidator, metricsCollector);

            server = createHttpServer(httpServerProperties);
            server.start();

            beaconService = new BeaconService(sdProperties, curatorClient);
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
            if (authValidationManager != null) {
                authValidationManager.stop();
            }
        } catch (Throwable t) {
            LOGGER.error("Error on stopping auth validation manager", t);
        }

        try {
            if (authManager != null) {
                authManager.stop();
            }
        } catch (Throwable t) {
            LOGGER.error("Error on auth manager shutdown", t);
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

    private static HttpServer createHttpServer(Properties httpServerProperties) {
        Properties throttlingProperties = PropertiesUtil.ofScope(httpServerProperties, Scopes.THROTTLING);

        CapacityThrottle<HttpServerRequest> throttle = new CapacityThrottle<>(
                throttlingProperties,
                new DefaultHttpServerRequestWeigher());
        metricsCollector.gauge("throttling.totalCapacity", throttle::totalCapacity);
        metricsCollector.gauge("throttling.availableCapacity", throttle::availableCapacity);

        ThrottledRequestProcessor<HttpServerRequest> throttledRequestProcessor = new DefaultThrottledHttpServerRequestProcessor();

        long maxContentLength = PropertiesUtil.get(HttpServer.Props.MAX_CONTENT_LENGTH, httpServerProperties).get();

        AuthProvider authProvider = new AuthProvider(new AdminAuthManager(Collections.emptySet()), authManager);
        HandlerWrapper authHandlerWrapper = new OrdinaryAuthHandlerWrapper(authProvider);

        HttpHandler sendAsyncHandler = authHandlerWrapper.wrap(
                new GateHandler(
                        authProvider,
                        throttle,
                        throttledRequestProcessor,
                        sendRequestProcessor,
                        authValidationManager,
                        streamStorage,
                        true,
                        maxContentLength,
                        metricsCollector));
        HttpHandler sendHandler = authHandlerWrapper.wrap(
                new GateHandler(
                        authProvider,
                        throttle,
                        throttledRequestProcessor,
                        sendRequestProcessor,
                        authValidationManager,
                        streamStorage,
                        false,
                        maxContentLength,
                        metricsCollector));

        RouteHandler handler = new InstrumentedRouteHandlerBuilder(httpServerProperties, metricsCollector).
                post("/stream/sendAsync", sendAsyncHandler).
                post("/stream/send", sendHandler).
                build();

        return new UndertowHttpServer(
                Application.application().getConfig().getHost(),
                Application.application().getConfig().getPort(),
                httpServerProperties,
                handler);
    }
}
