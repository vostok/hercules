package ru.kontur.vostok.hercules.gate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.application.Application;
import ru.kontur.vostok.hercules.auth.AdminAuthManager;
import ru.kontur.vostok.hercules.auth.AuthManager;
import ru.kontur.vostok.hercules.auth.AuthProvider;
import ru.kontur.vostok.hercules.auth.wrapper.OrdinaryAuthHandlerWrapper;
import ru.kontur.vostok.hercules.configuration.Scopes;
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
import java.util.Properties;

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
        Application.run("Hercules Gate", "gate", args, (properties, container) -> {
                Properties metricsProperties = PropertiesUtil.ofScope(properties, Scopes.METRICS);
                Properties curatorProperties = PropertiesUtil.ofScope(properties, Scopes.CURATOR);
                Properties validationProperties = PropertiesUtil.ofScope(properties, "validation");
                Properties eventSenderProperties = PropertiesUtil.ofScope(properties, "gate.event.sender");
                Properties sendRequestProcessorProperties = PropertiesUtil.ofScope(properties, "gate.send.request.processor");
                Properties httpServerProperties = PropertiesUtil.ofScope(properties, Scopes.HTTP_SERVER);
                Properties sdProperties = PropertiesUtil.ofScope(properties, Scopes.SERVICE_DISCOVERY);

                metricsCollector = container.register(new MetricsCollector(metricsProperties));
                CommonMetrics.registerCommonMetrics(metricsCollector);

                curatorClient = container.register(new CuratorClient(curatorProperties));

                authManager = container.register(new AuthManager(curatorClient));

                authValidationManager = container.register(new AuthValidationManager(curatorClient));

                streamStorage = container.register(new StreamStorage(new StreamRepository(curatorClient)));

                eventSender = container.register(new EventSender(eventSenderProperties, new HashPartitioner(new NaiveHasher()), metricsCollector));

                eventValidator = container.register(new EventValidator(validationProperties));

                sendRequestProcessor = container.register(new SendRequestProcessor(sendRequestProcessorProperties, eventSender, eventValidator, metricsCollector));

                server = container.register(createHttpServer(httpServerProperties));

                beaconService = container.register(new BeaconService(sdProperties, curatorClient));
            });
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
